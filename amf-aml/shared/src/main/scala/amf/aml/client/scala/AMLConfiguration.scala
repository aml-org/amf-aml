package amf.aml.client.scala

import amf.aml.client.scala.AMLConfiguration.platform
import amf.aml.client.scala.model.document.{Dialect, DialectInstance}
import amf.aml.client.scala.model.domain.SemanticExtension
import amf.aml.internal.annotations.serializable.AMLSerializableAnnotations
import amf.aml.internal.entities.AMLEntities
import amf.aml.internal.parse.plugin.{
  AMLDialectInstanceParsingPlugin,
  AMLDialectParsingPlugin,
  AMLVocabularyParsingPlugin
}
import amf.aml.internal.registries.AMLRegistry
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.aml.internal.render.plugin.{
  AMLDialectInstanceRenderingPlugin,
  AMLDialectRenderingPlugin,
  AMLVocabularyRenderingPlugin
}
import amf.aml.internal.transform.pipelines.{DefaultAMLTransformationPipeline, DialectTransformationPipeline}
import amf.aml.internal.utils.{DialectRegister, VocabulariesRegister}
import amf.aml.internal.validate.{AMFDialectValidations, AMLValidationPlugin}
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config._
import amf.core.client.scala.errorhandling.{
  AMFErrorHandler,
  DefaultErrorHandlerProvider,
  ErrorHandlerProvider,
  UnhandledErrorHandler
}
import amf.core.client.scala.execution.ExecutionEnvironment
import amf.core.client.scala.model.domain.AnnotationGraphLoader
import amf.core.client.scala.resource.ResourceLoader
import amf.core.client.scala.transform.{TransformationPipeline, TransformationPipelineRunner}
import amf.core.internal.annotations.serializable.CoreSerializableAnnotations
import amf.core.internal.entities.CoreEntities
import amf.core.internal.metamodel.ModelDefaultBuilder
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}
import amf.core.internal.plugins.AMFPlugin
import amf.core.internal.plugins.document.graph.entities.DataNodeEntities
import amf.core.internal.plugins.parse.DomainParsingFallback
import amf.core.internal.registries.AMFRegistry
import amf.core.internal.resource.AMFResolvers
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.internal.validation.EffectiveValidations
import amf.core.internal.validation.core.ValidationProfile
import org.mulesoft.common.collections.FilterType

import scala.concurrent.{ExecutionContext, Future}

/**
  * The configuration object required for using AML
  *
  * @param resolvers [[AMFResolvers]]
  * @param errorHandlerProvider [[ErrorHandlerProvider]]
  * @param registry [[AMLRegistry]]
  * @param listeners a Set of [[AMFEventListener]]
  * @param options [[AMFOptions]]
  */
class AMLConfiguration private[amf] (override private[amf] val resolvers: AMFResolvers,
                                     override private[amf] val errorHandlerProvider: ErrorHandlerProvider,
                                     override private[amf] val registry: AMLRegistry,
                                     override private[amf] val listeners: Set[AMFEventListener],
                                     override private[amf] val options: AMFOptions)
    extends AMFGraphConfiguration(resolvers, errorHandlerProvider, registry, listeners, options) {

  private implicit val ec: ExecutionContext = this.getExecutionContext

  override protected[amf] def copy(resolvers: AMFResolvers = resolvers,
                                   errorHandlerProvider: ErrorHandlerProvider = errorHandlerProvider,
                                   registry: AMFRegistry = registry,
                                   listeners: Set[AMFEventListener] = listeners,
                                   options: AMFOptions = options): AMLConfiguration =
    new AMLConfiguration(resolvers, errorHandlerProvider, registry.asInstanceOf[AMLRegistry], listeners, options)

  /** Contains common AMF graph operations associated to documents */
  override def baseUnitClient(): AMLBaseUnitClient = new AMLBaseUnitClient(this)

  /** Contains functionality associated with specific elements of the AMF model */
  override def elementClient(): AMLElementClient = new AMLElementClient(this)

  /** Contains methods to get information about the current state of the configuration */
  def configurationState(): AMLConfigurationState = new AMLConfigurationState(this)

  /**
    * Set [[ParsingOptions]]
    * @param parsingOptions [[ParsingOptions]] to add to configuration object
    * @return [[AMLConfiguration]] with [[ParsingOptions]] added
    */
  override def withParsingOptions(parsingOptions: ParsingOptions): AMLConfiguration =
    super._withParsingOptions(parsingOptions)

  /**
    * Add a [[ResourceLoader]]
    * @param rl [[ResourceLoader]] to add to configuration object
    * @return [[AMLConfiguration]] with the [[ResourceLoader]] added
    */
  override def withResourceLoader(rl: ResourceLoader): AMLConfiguration =
    super._withResourceLoader(rl)

  /**
    * Set the configuration [[ResourceLoader]]s
    * @param rl a list of [[ResourceLoader]] to set to the configuration object
    * @return [[AMLConfiguration]] with [[ResourceLoader]]s set
    */
  override def withResourceLoaders(rl: List[ResourceLoader]): AMLConfiguration =
    super._withResourceLoaders(rl)

  /**
    * Set [[UnitCache]]
    * @param cache [[UnitCache]] to add to configuration object
    * @return [[AMLConfiguration]] with [[UnitCache]] added
    */
  override def withUnitCache(cache: UnitCache): AMLConfiguration =
    super._withUnitCache(cache)

  override def withFallback(plugin: DomainParsingFallback): AMLConfiguration = super._withFallback(plugin)

  override def withPlugin(amfPlugin: AMFPlugin[_]): AMLConfiguration =
    super._withPlugin(amfPlugin)

  override def withPlugins(plugins: List[AMFPlugin[_]]): AMLConfiguration =
    super._withPlugins(plugins)

  private[amf] override def withValidationProfile(profile: ValidationProfile): AMLConfiguration =
    super._withValidationProfile(profile)

  // Keep AMF internal, done to avoid recomputing validations every time a config is requested
  private[amf] override def withValidationProfile(profile: ValidationProfile,
                                                  effective: EffectiveValidations): AMLConfiguration =
    super._withValidationProfile(profile, effective)

  /**
    * Add a [[TransformationPipeline]]
    * @param pipeline [[TransformationPipeline]] to add to configuration object
    * @return [[AMLConfiguration]] with [[TransformationPipeline]] added
    */
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

  /**
    * Set [[ErrorHandlerProvider]]
    * @param provider [[ErrorHandlerProvider]] to set to configuration object
    * @return [[AMLConfiguration]] with [[ErrorHandlerProvider]] set
    */
  override def withErrorHandlerProvider(provider: ErrorHandlerProvider): AMLConfiguration =
    super._withErrorHandlerProvider(provider)

  /**
    * Add an [[AMFEventListener]]
    * @param listener [[AMFEventListener]] to add to configuration object
    * @return [[AMLConfiguration]] with [[AMFEventListener]] added
    */
  override def withEventListener(listener: AMFEventListener): AMLConfiguration = super._withEventListener(listener)

  private[amf] override def withEntities(entities: Map[String, ModelDefaultBuilder]): AMLConfiguration =
    super._withEntities(entities)

  private[amf] override def withAnnotations(annotations: Map[String, AnnotationGraphLoader]): AMLConfiguration =
    super._withAnnotations(annotations)

  private[amf] def withExtensions(dialect: Dialect): AMLConfiguration = {
    copy(registry = registry.withExtensions(dialect))
  }

  /**
    * Set [[BaseExecutionEnvironment]]
    * @param executionEnv [[BaseExecutionEnvironment]] to set to configuration object
    * @return [[AMLConfiguration]] with [[BaseExecutionEnvironment]] set
    */
  override def withExecutionEnvironment(executionEnv: ExecutionEnvironment): AMLConfiguration =
    super._withExecutionEnvironment(executionEnv)

  /**
    * Register a Dialect
    * @param url URL of the Dialect to register
    * @return A CompletableFuture of [[AMLConfiguration]]
    */
  def withDialect(url: String): Future[AMLConfiguration] = {
    baseUnitClient().parseDialect(url).map {
      case result: AMLDialectResult => withDialect(result.dialect)
      case _                        => this
    }
  }

  /**
    * Register a Dialect
    * @param dialect [[Dialect]] to register
    * @return [[AMLConfiguration]] with [[Dialect]] registered
    */
  def withDialect(dialect: Dialect): AMLConfiguration = DialectRegister(dialect, this).register()

  /**
    * Register a [[Dialect]] linked from a [[DialectInstance]]
    * @param url of the [[DialectInstance]]
    * @return A CompletableFuture of [[AMLConfiguration]]
    */
  def forInstance(url: String): Future[AMLConfiguration] = {
    val collector = new DialectReferencesCollector
    val runner    = TransformationPipelineRunner(UnhandledErrorHandler, this)
    collector.collectFrom(url, this).map { dialects =>
      dialects
        .map { d =>
          runner.run(d, DialectTransformationPipeline())
          d
        }
        .foldLeft(this) { (config, dialect) =>
          val finder                                       = DefaultNodeMappableFinder(config).addDialect(dialect)
          val parsing: AMLDialectInstanceParsingPlugin     = new AMLDialectInstanceParsingPlugin(dialect)
          val rendering: AMLDialectInstanceRenderingPlugin = new AMLDialectInstanceRenderingPlugin(dialect)
          val profile                                      = new AMFDialectValidations(dialect)(finder).profile()
          config
            .withPlugins(List(parsing, rendering))
            .withValidationProfile(profile)
        }
    }
  }
}

object AMLConfiguration extends PlatformSecrets {

  /** Predefined environment to deal with AML documents based on AMFGraphConfiguration {@link amf.core.client.scala.AMFGraphConfiguration.predefined predefined()} method */
  def predefined(): AMLConfiguration = {
    val predefinedGraphConfiguration: AMFGraphConfiguration = AMFGraphConfiguration.predefined().emptyEntities()
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
        AMLRegistry(predefinedGraphConfiguration.getRegistry)
          .withEntities(AMLEntities.entities)
          .withAnnotations(AMLSerializableAnnotations.annotations),
        predefinedGraphConfiguration.listeners,
        predefinedGraphConfiguration.options
    ).withPlugins(predefinedPlugins)
      .withTransformationPipeline(DefaultAMLTransformationPipeline())
  }

  def empty(): AMLConfiguration = {
    new AMLConfiguration(
        AMFResolvers.predefined(),
        DefaultErrorHandlerProvider,
        AMLRegistry.empty,
        Set.empty,
        AMFOptions.default()
    )
  }
}

private class DialectReferencesCollector(implicit val ec: ExecutionContext) {
  def collectFrom(url: String, amfConfig: AMFGraphConfiguration): Future[Seq[Dialect]] = {
    val ctx      = new CompilerContextBuilder(url, platform, amfConfig.compilerConfiguration).build()
    val compiler = new AMFCompiler(ctx)
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
