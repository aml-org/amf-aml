package amf.plugins.document.vocabularies

import amf.client.plugins.{AMFDocumentPlugin, AMFPlugin, AMFValidationPlugin}
import amf.core.Root
import amf.core.client.ParsingOptions
import amf.core.emitter.RenderOptions
import amf.core.metamodel.Obj
import amf.core.model.document.BaseUnit
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.parser.{
  DefaultParserSideErrorHandler,
  ErrorHandler,
  ParserContext,
  ReferenceHandler,
  SyamlParsedDocument,
  _
}
import amf.core.rdf.RdfModel
import amf.core.registries.AMFDomainEntityResolver
import amf.core.remote.{Aml, Platform}
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.services.{RuntimeValidator, ValidationOptions}
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.ValidationProfile
import amf.core.validation.{AMFValidationReport, EffectiveValidations, SeverityLevels, ValidationResultProcessor}
import amf.internal.environment.Environment
import amf.plugins.document.vocabularies.AMLPlugin.{dialectHeaderDirective, registry}
import amf.plugins.document.vocabularies.annotations.{AliasesLocation, CustomId, JsonPointerRef, RefInclude}
import amf.plugins.document.vocabularies.emitters.dialects.{DialectEmitter, RamlDialectLibraryEmitter}
import amf.plugins.document.vocabularies.emitters.instances.DialectInstancesEmitter
import amf.plugins.document.vocabularies.emitters.vocabularies.VocabularyEmitter
import amf.plugins.document.vocabularies.metamodel.document._
import amf.plugins.document.vocabularies.metamodel.domain._
import amf.plugins.document.vocabularies.model.document._
import amf.plugins.document.vocabularies.parser.common.SyntaxExtensionsReferenceHandler
import amf.plugins.document.vocabularies.parser.dialects.{DialectContext, DialectsParser}
import amf.plugins.document.vocabularies.parser.instances.{DialectInstanceContext, DialectInstanceParser}
import amf.plugins.document.vocabularies.parser.vocabularies.{VocabulariesParser, VocabularyContext}
import amf.plugins.document.vocabularies.resolution.pipelines.{
  DialectInstancePatchResolutionPipeline,
  DialectInstanceResolutionPipeline,
  DialectResolutionPipeline
}
import amf.plugins.document.vocabularies.validation.AMFDialectValidations
import amf.{ProfileName, RamlProfile}
import org.yaml.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RamlHeaderExtractor {
  def comment(root: Root): Option[YComment] = {
    root.parsed match {
      case parsed: SyamlParsedDocument => parsed.comment
      case _                           => None
    }

  }

  def comment(document: YDocument): Option[YComment] = document.children.collectFirst({ case c: YComment => c })
}

trait JsonHeaderExtractor {
  def dialect(root: Root): Option[String] = {
    root.parsed match {
      case parsedInput: SyamlParsedDocument => dialectForDoc(parsedInput.document)
      case _                                => None
    }
  }

  private def dialectForDoc(document: YDocument): Option[String] = {
    document.node
      .toOption[YMap]
      .map(_.entries)
      .getOrElse(Nil)
      .collectFirst({ case e if e.key.asScalar.exists(_.text == "$dialect") => e })
      .flatMap(e => e.value.asScalar.map(_.text))
  }
}

trait KeyPropertyHeaderExtractor {
  def dialectByKeyProperty(root: YDocument): Option[Dialect] =
    registry
      .allDialects()
      .find(d => d.documents().keyProperty().value() && containsVersion(root, d))

  def dialectInKey(root: Root): Option[Dialect] =
    root.parsed match {
      case parsedInput: SyamlParsedDocument => dialectByKeyProperty(parsedInput.document)
      case _                                => None
    }

  private def containsVersion(document: YDocument, d: Dialect): Boolean =
    document.node
      .toOption[YMap]
      .map(_.entries)
      .getOrElse(Nil)
      .collectFirst({ case e if e.key.asScalar.exists(scalar => d.name().is(scalar.text)) => e })
      .exists(e => { e.value.asScalar.exists(_.text == d.version().value()) })
}

protected object ExtensionHeader {

  val VocabularyHeader = "%Vocabulary1.0"

  val DialectHeader = "%Dialect1.0"

  val DialectLibraryHeader = "%Library/Dialect1.0"

  val DialectFragmentHeader = "%NodeMapping/Dialect1.0"
}


object DialectHeader extends RamlHeaderExtractor with JsonHeaderExtractor with KeyPropertyHeaderExtractor {
  def apply(root: Root): Boolean = {
    val text = dialectHeaderDirective(root)

    if (isExternal(text)) true
    else {
      val header = text.map(h => h.split("\\|").head)

      header match {
        case Some(ExtensionHeader.DialectHeader) | Some(ExtensionHeader.DialectFragmentHeader) | Some(
                ExtensionHeader.DialectLibraryHeader) | Some(ExtensionHeader.VocabularyHeader) =>
          true
        case Some(other) => registry.findDialectForHeader(other).isDefined
        case _           => dialectInKey(root).isDefined
      }
    }
  }

  private def isExternal(header: Option[String]) = header.exists(h => h.endsWith(">") && h.contains("|<"))
}

object ReferenceStyles {
  val RAML             = "RamlStyle"
  val JSONSCHEMA       = "JsonSchemaStyle"
  val all: Seq[String] = Seq(RAML, JSONSCHEMA)
}

object AMLPlugin
    extends AMFDocumentPlugin
    with RamlHeaderExtractor
    with JsonHeaderExtractor
    with AMFValidationPlugin
    with ValidationResultProcessor
    with PlatformSecrets
    with KeyPropertyHeaderExtractor {

  val registry = new DialectsRegistry

  override val ID: String = Aml.name

  override val vendors: Seq[String] = Seq(Aml.name)

  override def init(): Future[AMFPlugin] = Future { this }

  override def modelEntities: Seq[Obj] = Seq(
      VocabularyModel,
      ExternalModel,
      VocabularyReferenceModel,
      ClassTermModel,
      ObjectPropertyTermModel,
      DatatypePropertyTermModel,
      DialectModel,
      NodeMappingModel,
      UnionNodeMappingModel,
      PropertyMappingModel,
      DocumentsModelModel,
      PublicNodeMappingModel,
      DocumentMappingModel,
      DialectLibraryModel,
      DialectFragmentModel,
      DialectInstanceModel,
      DialectInstanceLibraryModel,
      DialectInstanceFragmentModel,
      DialectInstancePatchModel
  )

  override def serializableAnnotations(): Map[String, AnnotationGraphLoader] =
    Map(
        "aliases-location" -> AliasesLocation,
        "custom-id"        -> CustomId,
        "ref-include"      -> RefInclude,
        "json-pointer-ref" -> JsonPointerRef
    )

  /**
    * Resolves the provided base unit model, according to the semantics of the domain of the document
    */
  override def resolve(unit: BaseUnit,
                       errorHandler: ErrorHandler,
                       pipelineId: String = ResolutionPipeline.DEFAULT_PIPELINE): BaseUnit =
    unit match {
      case patch: DialectInstancePatch =>
        new DialectInstancePatchResolutionPipeline(errorHandler).resolve(patch)
      case dialect: Dialect =>
        new DialectResolutionPipeline(errorHandler).resolve(dialect)
      case dialect: DialectInstance =>
        new DialectInstanceResolutionPipeline(errorHandler).resolve(dialect)
      case _ => unit
    }

  /**
    * List of media types used to encode serialisations of
    * this domain
    */
  override def documentSyntaxes: Seq[String] = Seq(
      "application/aml+json",
      "application/aml+yaml",
      "application/raml",
      "application/raml+json",
      "application/raml+yaml",
      "text/yaml",
      "text/x-yaml",
      "application/yaml",
      "application/x-yaml",
      "application/json"
  )

  /**
    * Parses an accepted document returning an optional BaseUnit
    */
  override def parse(document: Root,
                     parentContext: ParserContext,
                     platform: Platform,
                     options: ParsingOptions): Option[BaseUnit] = {

    val header = dialectHeaderDirective(document)

    header match {
      case Some(ExtensionHeader.VocabularyHeader) =>
        Some(new VocabulariesParser(document)(new VocabularyContext(parentContext)).parseDocument())
      case Some(ExtensionHeader.DialectLibraryHeader) =>
        Some(new DialectsParser(document)(new DialectContext(parentContext)).parseLibrary())
      case Some(ExtensionHeader.DialectFragmentHeader) =>
        Some(new DialectsParser(document)(new DialectContext(parentContext)).parseFragment())
      case Some(ExtensionHeader.DialectHeader) =>
        parseAndRegisterDialect(document, parentContext)
      case _ => parseDialectInstance(document, header, parentContext)
    }
  }

  /** Fetch header or $dialect directive. */
  def dialectHeaderDirective(document: Root): Option[String] = {
    val text = comment(document) match {
      case Some(comment) => Some(comment.metaText)
      case _             => dialect(document).map(metaText => s"%$metaText")
    }
    text.map(_.replace(" ", ""))
  }

  private def parseDialectInstance(root: Root, header: Option[String], parentContext: ParserContext) = {
    val h = header.map(h => h.split("\\|").head)

    val dialect = h.flatMap(registry.findDialectForHeader).orElse(dialectInKey(root))

    dialect match {
      case Some(d) => parseDocumentWithDialect(root, parentContext, d, h)
      case _       => throw new Exception(s"Unknown Dialect for document: ${root.location}")
    }
  }

  protected def unparseAsYDocument(unit: BaseUnit, renderOptions: RenderOptions): Option[YDocument] = {
    unit match {
      case vocabulary: Vocabulary  => Some(VocabularyEmitter(vocabulary).emitVocabulary())
      case dialect: Dialect        => Some(DialectEmitter(dialect).emitDialect())
      case library: DialectLibrary => Some(RamlDialectLibraryEmitter(library).emitDialectLibrary())
      case instance: DialectInstanceUnit =>
        Some(DialectInstancesEmitter(instance, registry.dialectFor(instance).get).emitInstance())
      case _ => None
    }
  }

  /**
    * Decides if this plugin can parse the provided document instance.
    * this can be used by multiple plugins supporting the same media-type
    * to decide which one will parse the document base on information from
    * the document structure
    */
  override def canParse(document: Root): Boolean =
    document.parsed.isInstanceOf[SyamlParsedDocument] && DialectHeader(document)

  /**
    * Decides if this plugin can unparse the provided model document instance.
    * this can be used by multiple plugins supporting the same media-type
    * to decide which one will unparse the document base on information from
    * the instance type and properties
    */
  override def canUnparse(unit: BaseUnit): Boolean = unit match {
    case _: Vocabulary     => true
    case _: Dialect        => true
    case _: DialectLibrary => true
    case instance: DialectInstanceUnit =>
      registry.knowsDialectInstance(instance)
    case _ => false
  }

  override def referenceHandler(eh: ErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(registry, eh)

  override def dependencies(): Seq[AMFPlugin] = Seq()

  override def modelEntitiesResolver: Option[AMFDomainEntityResolver] =
    Some(registry)

  private def parseAndRegisterDialect(document: Root, parentContext: ParserContext) = {
    new DialectsParser(document)(new DialectContext(parentContext))
      .parseDocument() match {
      case dialect: Dialect =>
        registry.register(dialect)
        Some(dialect)
      case unit => Some(unit)
    }
  }

  protected def parseDocumentWithDialect(document: Root,
                                         parentContext: ParserContext,
                                         dialect: Dialect,
                                         header: Option[String]): Option[DialectInstanceUnit] = {
    registry.withRegisteredDialect(dialect) { resolvedDialect =>
      header match {
        case Some(headerKey) if resolvedDialect.isFragmentHeader(headerKey) =>
          val name = headerKey.substring(1, headerKey.indexOf("/"))
          new DialectInstanceParser(document)(new DialectInstanceContext(resolvedDialect, parentContext))
            .parseFragment(name)
        case Some(headerKey) if resolvedDialect.isLibraryHeader(headerKey) =>
          new DialectInstanceParser(document)(new DialectInstanceContext(resolvedDialect, parentContext)).parseLibrary()
        case Some(headerKey) if resolvedDialect.isPatchHeader(headerKey) =>
          new DialectInstanceParser(document)(new DialectInstanceContext(resolvedDialect, parentContext).forPatch())
            .parsePatch()
        case _ =>
          new DialectInstanceParser(document)(new DialectInstanceContext(resolvedDialect, parentContext))
            .parseDocument()
      }
    }
  }

  /**
    * Validation profiles supported by this plugin. Notice this will be called multiple times.
    */
  override def domainValidationProfiles(platform: Platform): Map[String, () => ValidationProfile] = {
    registry.allDialects().foldLeft(Map[String, () => ValidationProfile]()) {
      case (acc, dialect) if !dialect.nameAndVersion().contains("Validation Profile") =>
        acc.updated(dialect.nameAndVersion(), () => {
          computeValidationProfile(dialect)
        })
      case (acc, _) => acc
    }
  }

  protected def computeValidationProfile(dialect: Dialect): ValidationProfile = {
    val header = dialect.header
    registry.validations.get(header) match {
      case Some(profile) => profile
      case _ =>
        val resolvedDialect = new DialectResolutionPipeline(DefaultParserSideErrorHandler(dialect)).resolve(dialect)
        val profile         = new AMFDialectValidations(resolvedDialect).profile()
        registry.validations += (header -> profile)
        profile
    }
  }

  def aggregateValidations(validations: EffectiveValidations,
                           dependenciesValidations: Seq[ValidationProfile]): EffectiveValidations = {
    dependenciesValidations.foldLeft(validations) {
      case (effective, profile) => effective.someEffective(profile)
    }
  }

  /**
    * Request for validation of a particular model, profile and list of effective validations for that profile
    */
  override def validationRequest(baseUnit: BaseUnit,
                                 profile: ProfileName,
                                 validations: EffectiveValidations,
                                 platform: Platform,
                                 env: Environment,
                                 resolved: Boolean): Future[AMFValidationReport] = {
    baseUnit match {
      case dialectInstance: DialectInstanceUnit =>
        val resolvedModel =
          new DialectInstanceResolutionPipeline(DefaultParserSideErrorHandler(baseUnit)).resolve(dialectInstance)

        val dependenciesValidations: Future[Seq[ValidationProfile]] = Future
          .sequence(dialectInstance.graphDependencies.map { instance =>
            registry.registerDialect(instance.value())
          }) map { dialects =>
          dialects.map(computeValidationProfile)
        }

        for {
          validationsFromDeps <- dependenciesValidations
          shaclReport <- RuntimeValidator.shaclValidation(resolvedModel,
                                                          aggregateValidations(validations, validationsFromDeps),
                                                          options = new ValidationOptions().withFullValidation())
        } yield {

          // adding model-side validations
          val results = shaclReport.results.flatMap { r =>
            buildValidationResult(baseUnit, r, RamlProfile.messageStyle, validations)
          }

          AMFValidationReport(
              conforms = !results.exists(_.level == SeverityLevels.VIOLATION),
              model = baseUnit.id,
              profile = profile,
              results = results
          )
        }

      case _ =>
        throw new Exception(s"Cannot resolve base unit of type ${baseUnit.getClass}")
    }
  }

  /**
    * Does references in this type of documents be recursive?
    */
  override val allowRecursiveReferences: Boolean = true

  def shapesForDialect(dialect: Dialect, validationFunctionsUrl: String): RdfModel = {
    val validationProfile = computeValidationProfile(dialect)
    val validations       = validationProfile.validations

    RuntimeValidator.shaclModel(validations, validationFunctionsUrl)
  }
}
