package amf.client.environment

import amf.client.environment.AMLConfiguration.{platform, predefined}
import amf.client.exported.config.AMFLogger
import amf.client.remod.amfcore.config._
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.registry.AMFRegistry
import amf.client.remod.parsing.{AMLDialectInstanceParsingPlugin, AMLDialectParsingPlugin, AMLVocabularyParsingPlugin}
import amf.client.remod.rendering.{
  AMLDialectInstanceRenderingPlugin,
  AMLDialectRenderingPlugin,
  AMLVocabularyRenderingPlugin
}
import amf.client.remod.{AMFGraphConfiguration, AMFParser, AMFResult, ErrorHandlerProvider}
import amf.core.errorhandling.{AMFErrorHandler, UnhandledErrorHandler}
import amf.core.metamodel.ModelDefaultBuilder
import amf.core.model.domain.DomainElement
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.resolution.pipelines.{TransformationPipeline, TransformationPipelineRunner}
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.ValidationProfile
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.internal.reference.UnitCache
import amf.internal.resource.ResourceLoader
import amf.plugins.document.vocabularies.{AMLValidationPlugin, DialectRegister}
import amf.plugins.document.vocabularies.annotations.serializable.AMLSerializableAnnotations
import amf.plugins.document.vocabularies.custom.ParsedValidationProfile
import amf.plugins.document.vocabularies.emitters.instances.DefaultNodeMappableFinder
import amf.plugins.document.vocabularies.entities.AMLEntities
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance}
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, SemanticExtension}
import amf.plugins.document.vocabularies.resolution.pipelines.{
  DefaultAMLTransformationPipeline,
  DialectTransformationPipeline
}
import amf.plugins.document.vocabularies.validation.AMFDialectValidations
import amf.plugins.domain.VocabulariesRegister
import amf.validation.ValidationDialectText
import org.mulesoft.common.collections.FilterType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * The configuration object required for using AML
  *
  * @param resolvers [[AMFResolvers]]
  * @param errorHandlerProvider [[ErrorHandlerProvider]]
  * @param registry [[AMFRegistry]]
  * @param logger [[AMFLogger]]
  * @param listeners a Set of [[AMFEventListener]]
  * @param options [[AMFOptions]]
  */
class AMLConfiguration private[amf] (override private[amf] val resolvers: AMFResolvers,
                                     override private[amf] val errorHandlerProvider: ErrorHandlerProvider,
                                     override private[amf] val registry: AMFRegistry,
                                     override private[amf] val logger: AMFLogger,
                                     override private[amf] val listeners: Set[AMFEventListener],
                                     override private[amf] val options: AMFOptions)
    extends AMFGraphConfiguration(resolvers, errorHandlerProvider, registry, logger, listeners, options) {

  private[amf] val PROFILE_DIALECT_URL = "http://a.ml/dialects/profile.raml"

  override protected def copy(resolvers: AMFResolvers,
                              errorHandlerProvider: ErrorHandlerProvider,
                              registry: AMFRegistry,
                              logger: AMFLogger,
                              listeners: Set[AMFEventListener],
                              options: AMFOptions): AMLConfiguration =
    new AMLConfiguration(resolvers, errorHandlerProvider, registry, logger, listeners, options)

  override def createClient(): AMLClient = new AMLClient(this)

  override def withParsingOptions(parsingOptions: ParsingOptions): AMLConfiguration =
    super._withParsingOptions(parsingOptions)

  override def withResourceLoader(rl: ResourceLoader): AMLConfiguration =
    super._withResourceLoader(rl)

  override def withResourceLoaders(rl: List[ResourceLoader]): AMLConfiguration =
    super._withResourceLoaders(rl)

  override def withUnitCache(cache: UnitCache): AMLConfiguration =
    super._withUnitCache(cache)

  override def withPlugin(amfPlugin: AMFPlugin[_]): AMLConfiguration =
    super._withPlugin(amfPlugin)

  override def withPlugins(plugins: List[AMFPlugin[_]]): AMLConfiguration =
    super._withPlugins(plugins)

  override def withValidationProfile(profile: ValidationProfile): AMLConfiguration =
    super._withValidationProfile(profile)

  override def withTransformationPipeline(pipeline: TransformationPipeline): AMLConfiguration =
    super._withTransformationPipeline(pipeline)

  /**
    * AMF internal method just to facilitate the construction
    * @param pipelines
    * @return
    */
  override private[amf] def withTransformationPipelines(pipelines: List[TransformationPipeline]): AMLConfiguration =
    super._withTransformationPipelines(pipelines)

  override def withRenderOptions(renderOptions: RenderOptions): AMLConfiguration =
    super._withRenderOptions(renderOptions)

  override def withErrorHandlerProvider(provider: ErrorHandlerProvider): AMLConfiguration =
    super._withErrorHandlerProvider(provider)

  override def withEventListener(listener: AMFEventListener): AMLConfiguration = super._withEventListener(listener)

  override def withLogger(logger: AMFLogger): AMLConfiguration = super._withLogger(logger)

  override def withEntities(entities: Map[String, ModelDefaultBuilder]): AMLConfiguration =
    super._withEntities(entities)

  def withExtensions(extensions: Seq[SemanticExtension]): AMLConfiguration =
    copy(registry = registry.withExtensions(extensions))

  override def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): AMLConfiguration =
    super._withAnnotations(annotations)

  def merge(other: AMLConfiguration): AMLConfiguration = super._merge(other)

  def withDialect(path: String): Future[AMLConfiguration] = {
    createClient().parse(path).map {
      case AMFResult(d: Dialect, _) => withDialect(d)
      case _                        => this
    }
  }

  def withDialect(dialect: Dialect): AMLConfiguration = DialectRegister(dialect).register(this)

  def withCustomValidationsEnabled(): Future[AMLConfiguration] = {
    AMFParser.parseContent(ValidationDialectText.text, PROFILE_DIALECT_URL, None, this) map {
      case AMFResult(d: Dialect, _) => withDialect(d)
      case _                        => this
    }
  }

  def withCustomProfile(profile: ValidationProfile): AMLConfiguration = {
    copy(registry = this.registry.withConstraints(profile))
  }

  def withCustomProfile(instancePath: String): Future[AMLConfiguration] = {
    // TODO: should check that ValidationProfile dialect is defined first?
    AMFParser.parse(instancePath, this).map {
      case AMFResult(parsed: DialectInstance, _) =>
        if (parsed.definedBy().is(PROFILE_DIALECT_URL)) {
          val profile = ParsedValidationProfile(parsed.encodes.asInstanceOf[DialectDomainElement])
          withCustomProfile(profile)
        } else {
          // TODO: throw exception?
          this
        }
      case _ => this
    }
  }

  // TODO: what about nested $dialect references?
  def forInstance(url: String, mediaType: Option[String] = None): Future[AMLConfiguration] = {
    val env       = predefined()
    val collector = new DialectReferencesCollector
    val runner    = TransformationPipelineRunner(UnhandledErrorHandler)
    collector.collectFrom(url, mediaType, this).map { dialects =>
      dialects
        .map { d =>
          runner.run(d, DialectTransformationPipeline())
          d
        }
        .foldLeft(this) { (env, dialect) =>
          val finder                                       = DefaultNodeMappableFinder(this).addDialect(dialect)
          val parsing: AMLDialectInstanceParsingPlugin     = new AMLDialectInstanceParsingPlugin(dialect)
          val rendering: AMLDialectInstanceRenderingPlugin = new AMLDialectInstanceRenderingPlugin(dialect)
          val profile                                      = new AMFDialectValidations(dialect)(finder).profile()
          env
            .withPlugins(List(parsing, rendering))
            .withValidationProfile(profile)
        }
    }
  }
}

object AMLConfiguration extends PlatformSecrets {

  /** Predefined environment to deal with AML documents based on AMFGraphConfiguration {@link amf.client.remod.AMFGraphConfiguration.predefined predefined()} method */
  def predefined(): AMLConfiguration = {
    val predefinedGraphConfiguration: AMFGraphConfiguration = AMFGraphConfiguration.predefined()
    VocabulariesRegister.register() // TODO ARM remove when APIMF-3000 is done

    val predefinedPlugins = new AMLDialectParsingPlugin() ::
      new AMLVocabularyParsingPlugin() ::
      new AMLDialectRenderingPlugin() ::
      new AMLVocabularyRenderingPlugin() ::
      new AMLValidationPlugin() ::
      Nil

    // we might need to register editing pipeline as well because of legacy behaviour.
    new AMLConfiguration(
        predefinedGraphConfiguration.resolvers,
        predefinedGraphConfiguration.errorHandlerProvider,
        predefinedGraphConfiguration.registry
          .withEntities(AMLEntities.entities)
          .withAnnotations(AMLSerializableAnnotations.annotations),
        predefinedGraphConfiguration.logger,
        predefinedGraphConfiguration.listeners,
        predefinedGraphConfiguration.options
    ).withPlugins(predefinedPlugins)
      .withTransformationPipeline(DefaultAMLTransformationPipeline())
  }
  //TODO ARM remove
  private[amf] def forEH(eh: AMFErrorHandler) = {
    predefined().withErrorHandlerProvider(() => eh)
  }
}

class DialectReferencesCollector {
  def collectFrom(url: String,
                  mediaType: Option[String] = None,
                  amfConfig: AMFGraphConfiguration): Future[Seq[Dialect]] = {
    // todo
    val ctx      = new CompilerContextBuilder(url, platform, amfConfig.parseConfiguration).build()
    val compiler = new AMFCompiler(ctx, mediaType)
    for {
      content                <- compiler.fetchContent()
      eitherContentOrAst     <- Future.successful(compiler.parseSyntax(content))
      root                   <- Future.successful(eitherContentOrAst.right.get) if eitherContentOrAst.isRight
      plugin                 <- Future.successful(compiler.getDomainPluginFor(root))
      documentWithReferences <- compiler.parseReferences(root, plugin.get) if plugin.isDefined
    } yield {
      documentWithReferences.references.toStream
        .map(_.unit)
        .filterType[Dialect]
    }
  }
}
