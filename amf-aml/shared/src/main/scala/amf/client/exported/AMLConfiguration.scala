package amf.client.exported

import amf.client.environment.{AMLConfiguration => InternalAMLConfiguration}
import amf.client.resolve.ClientErrorHandlerConverter._
import amf.client.convert.VocabulariesClientConverter._
import amf.client.convert.TransformationPipelineConverter._
import amf.client.exported.config.{AMFEventListener, AMFLogger, ParsingOptions, RenderOptions}
import amf.client.exported.transform.TransformationPipeline
import amf.client.model.document.Dialect
import amf.client.reference.UnitCache
import amf.client.resource.ResourceLoader

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class AMLConfiguration private[amf] (private[amf] override val _internal: InternalAMLConfiguration)
    extends AMFGraphConfiguration(_internal) {
  private implicit val ec: ExecutionContext = _internal.getExecutionContext

  override def createClient(): AMLClient = new AMLClient(this)

  override def withParsingOptions(parsingOptions: ParsingOptions): AMLConfiguration =
    _internal.withParsingOptions(parsingOptions)

  override def withRenderOptions(renderOptions: RenderOptions): AMLConfiguration =
    _internal.withRenderOptions(renderOptions)
  //TODO FIX EH
//  override def withErrorHandlerProvider(provider: ErrorHandlerProvider): AMLConfiguration =
//    _internal.withErrorHandlerProvider(() => provider.errorHandler())

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

  /**
    * Merges two environments taking into account specific attributes that can be merged.
    * This is currently limited to: registry plugins, registry transformation pipelines.
    */
  def merge(other: AMLConfiguration): AMLConfiguration = _internal.merge(other)

  def withDialect(path: String): ClientFuture[AMLConfiguration] = _internal.withDialect(path).asClient

  def withDialect(dialect: Dialect): AMLConfiguration = _internal.withDialect(dialect)

  def withCustomProfile(instancePath: String): ClientFuture[AMLConfiguration] =
    _internal.withCustomProfile(instancePath).asClient

  def forInstance(url: String): ClientFuture[AMLConfiguration] = _internal.forInstance(url).asClient
}

@JSExportAll
@JSExportTopLevel("AMLConfiguration")
object AMLConfiguration {

  /** Predefined environment to deal with AML documents based on AMLConfiguration predefined() method */
  def predefined(): AMLConfiguration = InternalAMLConfiguration.predefined()
}
