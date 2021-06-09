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
import amf.client.validate.ValidationProfile

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class AMLConfiguration private[amf] (private[amf] override val _internal: InternalAMLConfiguration)
    extends BaseAMLConfiguration(_internal) {

  override def createClient(): AMLClient = new AMLClient(this)

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

  /**
    * Merges two environments taking into account specific attributes that can be merged.
    * This is currently limited to: registry plugins, registry transformation pipelines.
    */
  def merge(other: AMLConfiguration): AMLConfiguration = _internal.merge(other)

  def withCustomValidationsEnabled(): ClientFuture[AMLConfiguration] =
    _internal.withCustomValidationsEnabled().asClient

  def withDialect(path: String): ClientFuture[AMLConfiguration] = _internal.withDialect(path).asClient

  def withCustomProfile(instancePath: String): ClientFuture[AMLConfiguration] =
    _internal.withCustomProfile(instancePath).asClient

  def withCustomProfile(profile: ValidationProfile): AMLConfiguration =
    _internal.withCustomProfile(profile)

  def forInstance(url: String): ClientFuture[AMLConfiguration] = _internal.forInstance(url).asClient
}

@JSExportAll
@JSExportTopLevel("AMLConfiguration")
object AMLConfiguration {

  /** Predefined environment to deal with AML documents based on AMLConfiguration predefined() method */
  def predefined(): AMLConfiguration = InternalAMLConfiguration.predefined()
}
