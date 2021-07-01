package amf.aml.client.platform

import amf.aml.client.platform.model.document.Dialect
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.common.validation.ValidationProfile
import amf.core.client.platform.config.{AMFEventListener, AMFLogger, ParsingOptions, RenderOptions}
import amf.core.client.platform.errorhandling.ErrorHandlerProvider
import amf.core.client.platform.reference.UnitCache
import amf.core.client.platform.resource.ResourceLoader
import amf.core.client.platform.transform.TransformationPipeline
import amf.core.internal.convert.ClientErrorHandlerConverter._
import amf.core.internal.convert.TransformationPipelineConverter._
import amf.aml.client.scala.{AMLConfiguration => InternalAMLConfiguration}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.aml.client.scala
import amf.core.client.platform.execution.BaseExecutionEnvironment

@JSExportAll
class AMLConfiguration private[amf] (private[amf] override val _internal: scala.AMLConfiguration)
    extends BaseAMLConfiguration(_internal) {

  override def baseUnitClient(): AMLBaseUnitClient = new AMLBaseUnitClient(this)
  def elementClient(): AMLElementClient            = new AMLElementClient(this)

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
    _internal.withUnitCache(ReferenceResolverMatcher.asInternal(cache))

  override def withTransformationPipeline(pipeline: TransformationPipeline): AMLConfiguration =
    _internal.withTransformationPipeline(pipeline)

  override def withEventListener(listener: AMFEventListener): AMLConfiguration = _internal.withEventListener(listener)

  override def withLogger(logger: AMFLogger): AMLConfiguration = _internal.withLogger(logger)

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
}

@JSExportAll
@JSExportTopLevel("AMLConfiguration")
object AMLConfiguration {

  def empty(): AMLConfiguration = InternalAMLConfiguration.empty()

  /** Predefined environment to deal with AML documents based on AMLConfiguration predefined() method */
  def predefined(): AMLConfiguration = InternalAMLConfiguration.predefined()
}
