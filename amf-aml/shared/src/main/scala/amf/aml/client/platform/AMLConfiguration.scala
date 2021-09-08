package amf.aml.client.platform

import amf.aml.client.platform.model.document.Dialect
import amf.aml.client.scala.{AMLConfiguration => InternalAMLConfiguration}
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.config.{AMFEventListener, ParsingOptions, RenderOptions}
import amf.core.client.platform.errorhandling.ErrorHandlerProvider
import amf.core.client.platform.execution.BaseExecutionEnvironment
import amf.core.client.platform.reference.UnitCache
import amf.core.client.platform.resource.ResourceLoader
import amf.core.client.platform.transform.TransformationPipeline
import amf.core.client.platform.validation.payload.AMFShapePayloadValidationPlugin
import amf.core.internal.convert.ClientErrorHandlerConverter._
import amf.core.internal.convert.PayloadValidationPluginConverter.PayloadValidationPluginMatcher
import amf.core.internal.convert.TransformationPipelineConverter._

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/** The configuration object required for using AML */
@JSExportAll
class AMLConfiguration private[amf] (private[amf] override val _internal: InternalAMLConfiguration)
    extends BaseAMLConfiguration(_internal) {

  /** Contains common AMF graph operations associated to documents */
  override def baseUnitClient(): AMLBaseUnitClient = new AMLBaseUnitClient(this)

  /** Contains functionality associated with specific elements of the AMF model */
  override def elementClient(): AMLElementClient = new AMLElementClient(this)

  def configurationState(): AMLConfigurationState = new AMLConfigurationState(this)

  /**
    * Set [[ParsingOptions]]
    * @param parsingOptions [[ParsingOptions]] to add to configuration object
    * @return [[AMLConfiguration]] with [[ParsingOptions]] added
    */
  override def withParsingOptions(parsingOptions: ParsingOptions): AMLConfiguration =
    _internal.withParsingOptions(parsingOptions)

  override def withRenderOptions(renderOptions: RenderOptions): AMLConfiguration =
    _internal.withRenderOptions(renderOptions)

  override def withErrorHandlerProvider(provider: ErrorHandlerProvider): AMLConfiguration =
    _internal.withErrorHandlerProvider(() => provider.errorHandler())

  /**
    * Add a [[ResourceLoader]]
    * @param rl [[ResourceLoader]] to add to configuration object
    * @return [[AMLConfiguration]] with the [[ResourceLoader]] added
    */
  override def withResourceLoader(rl: ResourceLoader): AMLConfiguration =
    _internal.withResourceLoader(ResourceLoaderMatcher.asInternal(rl))

  /**
    * Set the configuration [[ResourceLoader]]s
    * @param rl a list of [[ResourceLoader]] to set to the configuration object
    * @return [[AMLConfiguration]] with [[ResourceLoader]]s set
    */
  override def withResourceLoaders(rl: ClientList[ResourceLoader]): AMLConfiguration =
    _internal.withResourceLoaders(rl.asInternal.toList)

  /**
    * Set [[UnitCache]]
    * @param cache [[UnitCache]] to add to configuration object
    * @return [[AMLConfiguration]] with [[UnitCache]] added
    */
  override def withUnitCache(cache: UnitCache): AMLConfiguration =
    _internal.withUnitCache(UnitCacheMatcher.asInternal(cache))

  /**
    * Add a [[TransformationPipeline]]
    * @param pipeline [[TransformationPipeline]] to add to configuration object
    * @return [[AMLConfiguration]] with [[TransformationPipeline]] added
    */
  override def withTransformationPipeline(pipeline: TransformationPipeline): AMLConfiguration =
    _internal.withTransformationPipeline(pipeline)

  /**
    * Add an [[AMFEventListener]]
    * @param listener [[AMFEventListener]] to add to configuration object
    * @return [[AMLConfiguration]] with [[AMFEventListener]] added
    */
  override def withEventListener(listener: AMFEventListener): AMLConfiguration = _internal.withEventListener(listener)

  /**
    * Set [[BaseExecutionEnvironment]]
    * @param executionEnv [[BaseExecutionEnvironment]] to set to configuration object
    * @return [[AMLConfiguration]] with [[BaseExecutionEnvironment]] set
    */
  override def withExecutionEnvironment(executionEnv: BaseExecutionEnvironment): AMLConfiguration =
    _internal.withExecutionEnvironment(executionEnv._internal)

  /**
    * Register a Dialect
    * @param dialect [[Dialect]] to register
    * @return [[AMLConfiguration]] with [[Dialect]] registered
    */
  override def withDialect(dialect: Dialect): AMLConfiguration = _internal.withDialect(dialect)

  /**
    * Register a Dialect
    * @param url URL of the Dialect to register
    * @return A CompletableFuture of [[AMLConfiguration]]
    */
  def withDialect(url: String): ClientFuture[AMLConfiguration] = _internal.withDialect(url).asClient

  def forInstance(url: String): ClientFuture[AMLConfiguration] = _internal.forInstance(url).asClient

  override def withShapePayloadPlugin(plugin: AMFShapePayloadValidationPlugin): AMLConfiguration =
    _internal.withPlugin(PayloadValidationPluginMatcher.asInternal(plugin))
}

@JSExportAll
@JSExportTopLevel("AMLConfiguration")
object AMLConfiguration {

  def empty(): AMLConfiguration = InternalAMLConfiguration.empty()

  /** Predefined environment to deal with AML documents based on AMLConfiguration predefined() method */
  def predefined(): AMLConfiguration = InternalAMLConfiguration.predefined()
}
