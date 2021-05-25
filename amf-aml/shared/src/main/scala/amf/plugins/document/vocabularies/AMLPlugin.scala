package amf.plugins.document.vocabularies

import amf.client.plugins.{AMFDocumentPlugin, AMFPlugin, AMFValidationPlugin}
import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.validate.AMFValidatePlugin
import amf.core.Root
import amf.core.annotations.Aliases
import amf.core.errorhandling.AMFErrorHandler
import amf.core.metamodel.Obj
import amf.core.model.document.BaseUnit
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.parser.{ParserContext, ReferenceHandler, _}
import amf.core.rdf.RdfModel
import amf.core.registries.{AMFDomainEntityResolver, AMFPluginsRegistry}
import amf.core.remote.Aml
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.services.RuntimeValidator
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.ShaclReportAdaptation
import amf.core.validation.core.ValidationProfile
import amf.core.vocabulary.NamespaceAliases
import amf.plugins.document.vocabularies.AMLValidationLegacyPlugin.amlPlugin
import amf.plugins.document.vocabularies.annotations.serializable.AMLSerializableAnnotations
import amf.plugins.document.vocabularies.emitters.dialects.{DialectEmitter, RamlDialectLibraryEmitter}
import amf.plugins.document.vocabularies.emitters.instances.DialectInstancesEmitter
import amf.plugins.document.vocabularies.emitters.vocabularies.VocabularyEmitter
import amf.plugins.document.vocabularies.entities.AMLEntities
import amf.plugins.document.vocabularies.model.document._
import amf.plugins.document.vocabularies.parser.common.SyntaxExtensionsReferenceHandler
import amf.plugins.document.vocabularies.parser.dialects.{DialectContext, DialectsParser}
import amf.plugins.document.vocabularies.parser.instances._
import amf.plugins.document.vocabularies.parser.vocabularies.{VocabulariesParser, VocabularyContext}
import amf.plugins.document.vocabularies.plugin.headers._
import amf.plugins.document.vocabularies.resolution.pipelines.{AMLEditingPipeline, DefaultAMLTransformationPipeline}
import org.yaml.model._

import scala.concurrent.{ExecutionContext, Future}

object AMLPlugin extends AMLPlugin {
  def apply(): AMLPlugin =
    AMFPluginsRegistry.documentPluginForID(this.ID).collect({ case a: AMLPlugin => a }).getOrElse(this)
}

trait AMLPlugin
    extends AMFDocumentPlugin
    with RamlHeaderExtractor
    with JsonHeaderExtractor
    with AMFValidationPlugin
    with ShaclReportAdaptation
    with PlatformSecrets
    with KeyPropertyHeaderExtractor {

  val registry = new DialectsRegistry

  override val ID: String = Aml.name

  override val vendors: Seq[String] = Seq(Aml.name)

  override def init()(implicit executionContext: ExecutionContext): Future[AMFPlugin] = Future { this }

  override def modelEntities: Seq[Obj] = AMLEntities.entities.values.toSeq

  override def serializableAnnotations(): Map[String, AnnotationGraphLoader] = AMLSerializableAnnotations.annotations

  override val pipelines: Map[String, TransformationPipeline] = Map(
      DefaultAMLTransformationPipeline.name -> DefaultAMLTransformationPipeline(),
      AMLEditingPipeline.name               -> AMLEditingPipeline() // hack to maintain compatibility with legacy behaviour
  )

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
  override def parse(document: Root, ctx: ParserContext): BaseUnit = {

    val header = DialectHeader.dialectHeaderDirective(document)

    header match {
      case Some(ExtensionHeader.VocabularyHeader) =>
        new VocabulariesParser(document)(new VocabularyContext(ctx)).parseDocument()
      case Some(ExtensionHeader.DialectLibraryHeader) =>
        new DialectsParser(document)(cleanDialectContext(ctx, document)).parseLibrary()
      case Some(ExtensionHeader.DialectFragmentHeader) =>
        new DialectsParser(document)(new DialectContext(ctx)).parseFragment()
      case Some(ExtensionHeader.DialectHeader) =>
        parseDialect(document, cleanDialectContext(ctx, document))
      case _ => parseDialectInstance(document, header, ctx)
    }
  }

  private def parseDialectInstance(root: Root, header: Option[String], parentContext: ParserContext) = {
    val h = header.map(h => h.split("\\|").head)

    val dialect = h.flatMap(registry.findDialectForHeader).orElse(dialectInKey(root, registry))

    dialect match {
      case Some(d) => parseDocumentWithDialect(root, parentContext, d, h)
      case _       => throw new Exception(s"Unknown Dialect for document: ${root.location}")
    }
  }

  protected def unparseAsYDocument(unit: BaseUnit,
                                   renderOptions: RenderOptions,
                                   errorHandler: AMFErrorHandler): Option[YDocument] = {
    throw new UnsupportedOperationException("Message by pope: use the new render config :D")
  }

  /**
    * Decides if this plugin can parse the provided document instance.
    * this can be used by multiple plugins supporting the same media-type
    * to decide which one will parse the document base on information from
    * the document structure
    */
  override def canParse(document: Root): Boolean = false

  /**
    * Decides if this plugin can unparse the provided model document instance.
    * this can be used by multiple plugins supporting the same media-type
    * to decide which one will unparse the document base on information from
    * the instance type and properties
    */
  override def canUnparse(unit: BaseUnit): Boolean = false

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(registry, eh)

  override def dependencies(): Seq[AMFPlugin] = Seq()

  override val validVendorsToReference: Seq[String] = Nil

  override def modelEntitiesResolver: Option[AMFDomainEntityResolver] =
    Some(registry)

  private def parseDialect(document: Root, parentContext: ParserContext) =
    new DialectsParser(document)(new DialectContext(parentContext)).parseDocument()

  override protected[amf] def getRemodValidatePlugins(): Seq[AMFValidatePlugin] = Seq(amlPlugin())

  protected def parseDocumentWithDialect(document: Root,
                                         parentContext: ParserContext,
                                         dialect: Dialect,
                                         header: Option[String]): DialectInstanceUnit = {
    registry.withRegisteredDialect(dialect) { resolvedDialect =>
      header match {
        case Some(headerKey) if resolvedDialect.isFragmentHeader(headerKey) =>
          val name = headerKey.substring(1, headerKey.indexOf("/"))
          new DialectInstanceFragmentParser(document)(new DialectInstanceContext(resolvedDialect, parentContext))
            .parse(name)
        case Some(headerKey) if resolvedDialect.isLibraryHeader(headerKey) =>
          new DialectInstanceLibraryParser(document)(new DialectInstanceContext(resolvedDialect, parentContext))
            .parse()
        case Some(headerKey) if resolvedDialect.isPatchHeader(headerKey) =>
          new DialectInstancePatchParser(document)(
              new DialectInstanceContext(resolvedDialect, parentContext).forPatch())
            .parse()
        case _ =>
          new DialectInstanceParser(document)(new DialectInstanceContext(resolvedDialect, parentContext))
            .parseDocument()
      }
    }
  }

  /**
    * Validation profiles supported by this plugin. Notice this will be called multiple times.
    */
  override def domainValidationProfiles: Seq[ValidationProfile] = registry.env().registry.constraintsRules.values.toSeq

  protected def computeValidationProfile(dialect: Dialect): ValidationProfile = {
    DialectValidationProfileComputation.computeProfileFor(dialect, registry)
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

  // context that opens a new context for declarations and copies the global JSON Schema declarations
  protected def cleanDialectContext(wrapped: ParserContext, root: Root): DialectContext = {
    val cleanNested =
      ParserContext(root.location, root.references, EmptyFutureDeclarations(), wrapped.config)
    new DialectContext(cleanNested)
  }
}
