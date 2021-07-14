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

@JSExportAll
class AMLConfiguration private[amf] (private[amf] override val _internal: InternalAMLConfiguration)
    extends BaseAMLConfiguration(_internal) {

  override def baseUnitClient(): AMLBaseUnitClient = new AMLBaseUnitClient(this)
  def elementClient(): AMLElementClient            = new AMLElementClient(this)
  def configurationState(): AMLConfigurationState  = new AMLConfigurationState(this)

  override def withParsingOptions(parsingOptions: ParsingOptions): AMLConfiguration =
    _internal.withParsingOptions(parsingOptions)

  override def withRenderOptions(renderOptions: RenderOptions): AMLConfiguration =
    _internal.withRenderOptions(renderOptions)

  override def withErrorHandlerProvider(provider: ErrorHandlerProvider): AMLConfiguration =
    _internal.withErrorHandlerProvider(() => provider.errorHandler())

  override def withResourceLoader(rl: ResourceLoader): AMLConfiguration =
    _internal.withResourceLoader(ResourceLoaderMatcher.asInternal(rl))

  override def withResourceLoaders(rl: ClientList[ResourceLoader]): AMLConfiguration =
    _internal.withResourceLoaders(rl.asInternal.toList)

  override def withUnitCache(cache: UnitCache): AMLConfiguration =
    _internal.withUnitCache(UnitCacheMatcher.asInternal(cache))

  override def withTransformationPipeline(pipeline: TransformationPipeline): AMLConfiguration =
    _internal.withTransformationPipeline(pipeline)

  override def withEventListener(listener: AMFEventListener): AMLConfiguration = _internal.withEventListener(listener)

  override def withDialect(dialect: Dialect): AMLConfiguration = _internal.withDialect(dialect)

  override def withExecutionEnvironment(executionEnv: BaseExecutionEnvironment): AMLConfiguration =
    _internal.withExecutionEnvironment(executionEnv._internal)

  /**
    * Merges two environments taking into account specific attributes that can be merged.
    * This is currently limited to: registry plugins, registry transformation pipelines.
    */
  def merge(other: AMLConfiguration): AMLConfiguration = _internal.merge(other)

  def withDialect(path: String): ClientFuture[AMLConfiguration] = _internal.withDialect(path).asClient

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
