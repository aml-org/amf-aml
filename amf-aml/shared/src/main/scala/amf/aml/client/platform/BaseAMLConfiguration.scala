package amf.aml.client.platform

import amf.aml.client.platform.model.document.Dialect
import amf.aml.client.scala.{AMLConfiguration => InternalAMLConfiguration}
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.AMFGraphConfiguration
import amf.core.client.platform.config.{AMFEventListener, ParsingOptions, RenderOptions}
import amf.core.client.platform.errorhandling.ErrorHandlerProvider
import amf.core.client.platform.reference.UnitCache
import amf.core.client.platform.resource.ResourceLoader
import amf.core.client.platform.transform.TransformationPipeline
import amf.core.internal.convert.ClientErrorHandlerConverter._
import amf.core.internal.convert.TransformationPipelineConverter._

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class BaseAMLConfiguration private[amf] (private[amf] override val _internal: InternalAMLConfiguration)
    extends AMFGraphConfiguration(_internal) {

  protected implicit val ec: ExecutionContext = _internal.getExecutionContext

  override def withParsingOptions(parsingOptions: ParsingOptions): BaseAMLConfiguration =
    new BaseAMLConfiguration(_internal.withParsingOptions(parsingOptions))

  override def withRenderOptions(renderOptions: RenderOptions): BaseAMLConfiguration =
    new BaseAMLConfiguration(_internal.withRenderOptions(renderOptions))

  override def withErrorHandlerProvider(provider: ErrorHandlerProvider): BaseAMLConfiguration =
    new BaseAMLConfiguration(_internal.withErrorHandlerProvider(() => provider.errorHandler()))

  override def withResourceLoader(rl: ResourceLoader): BaseAMLConfiguration =
    new BaseAMLConfiguration(_internal.withResourceLoader(ResourceLoaderMatcher.asInternal(rl)))

  override def withResourceLoaders(rl: ClientList[ResourceLoader]): BaseAMLConfiguration =
    new BaseAMLConfiguration(_internal.withResourceLoaders(rl.asInternal.toList))

  override def withUnitCache(cache: UnitCache): BaseAMLConfiguration =
    new BaseAMLConfiguration(_internal.withUnitCache(UnitCacheMatcher.asInternal(cache)))

  override def withTransformationPipeline(pipeline: TransformationPipeline): BaseAMLConfiguration =
    new BaseAMLConfiguration(_internal.withTransformationPipeline(pipeline))

  override def withEventListener(listener: AMFEventListener): BaseAMLConfiguration =
    new BaseAMLConfiguration(_internal.withEventListener(listener))

  def withDialect(dialect: Dialect): BaseAMLConfiguration =
    new BaseAMLConfiguration(_internal.withDialect(dialect))
}
