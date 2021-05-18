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
import amf.client.remod.{AMFGraphConfiguration, AMFResult, ErrorHandlerProvider, ParseConfiguration}
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.resolution.pipelines.{TransformationPipeline, TransformationPipelineRunner}
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.ValidationProfile
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.internal.reference.UnitCache
import amf.internal.resource.ResourceLoader
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.annotations.serializable.AMLSerializableAnnotations
import amf.plugins.document.vocabularies.entities.AMLEntities
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance}
import amf.plugins.document.vocabularies.resolution.pipelines.{
  DefaultAMLTransformationPipeline,
  DialectTransformationPipeline
}
import amf.plugins.document.vocabularies.validation.AMFDialectValidations
import org.mulesoft.common.collections.FilterType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * The configuration object required for using AML
  *
  * @param resolvers {@link amf.client.remod.amfcore.config.AMFResolvers}
  * @param errorHandlerProvider {@link amf.client.remod.ErrorHandlerProvider}
  * @param registry {@link amf.client.remod.amfcore.registry.AMFRegistry}
  * @param logger {@link amf.client.exported.config.AMFLogger}
  * @param listeners a Set of {@link amf.client.exported.config.AMFEventListener}
  * @param options {@link amf.client.remod.amfcore.config.AMFOptions}
  */
class AMLConfiguration private[amf] (override private[amf] val resolvers: AMFResolvers,
                                     override private[amf] val errorHandlerProvider: ErrorHandlerProvider,
                                     override private[amf] val registry: AMFRegistry,
                                     override private[amf] val logger: AMFLogger,
                                     override private[amf] val listeners: Set[AMFEventListener],
                                     override private[amf] val options: AMFOptions)
    extends AMFGraphConfiguration(resolvers, errorHandlerProvider, registry, logger, listeners, options) {

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

  def merge(other: AMLConfiguration): AMLConfiguration = super._merge(other)

  /**
    *
    * @param path
    * @return
    */
  def withDialect(path: String): Future[AMLConfiguration] = {
    createClient().parse(path).map {
      case AMFResult(d: Dialect, _) => withDialect(d)
      case _                        => this
    }
  }

  def withDialect(dialect: Dialect): AMLConfiguration = {
    AMLPlugin.registry.register(dialect)
    this
  }

  def withCustomProfile(instancePath: String): Future[AMLConfiguration] = {
    createClient().parse(instancePath: String).map {
      case AMFResult(i: DialectInstance, _) => throw new UnsupportedOperationException() // SET REGISTRY PROFILE
      case _                                => this
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
          val parsing: AMLDialectInstanceParsingPlugin     = new AMLDialectInstanceParsingPlugin(dialect)
          val rendering: AMLDialectInstanceRenderingPlugin = new AMLDialectInstanceRenderingPlugin(dialect)
          val profile                                      = new AMFDialectValidations(dialect).profile()
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

    val predefinedPlugins = new AMLDialectParsingPlugin() ::
      new AMLVocabularyParsingPlugin() ::
      new AMLDialectRenderingPlugin() ::
      new AMLVocabularyRenderingPlugin() ::
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
}

class DialectReferencesCollector {
  def collectFrom(url: String,
                  mediaType: Option[String] = None,
                  amfConfig: AMFGraphConfiguration): Future[Seq[Dialect]] = {
    // todo
    val ctx      = new CompilerContextBuilder(platform, new ParseConfiguration(amfConfig, url)).build()
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
