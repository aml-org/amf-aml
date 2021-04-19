package amf.client.environment

import amf.client.remod.amfcore.config._
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.plugins.parse.AMFParsePlugin
import amf.client.remod.amfcore.registry.AMFRegistry
import amf.client.remod.{AMFGraphConfiguration, AMFResult, ErrorHandlerProvider}
import amf.core.validation.core.ValidationProfile
import amf.internal.reference.UnitCache
import amf.internal.resource.ResourceLoader
import amf.plugins.document.graph.AMFGraphParsePlugin
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, DialectInstanceUnit}
import amf.plugins.document.vocabularies.{AMLInstancePlugin, AMLParsePlugin}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

private[amf] class AMLConfiguration(override private[amf] val resolvers: AMFResolvers,
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

  override def withParsingOptions(parsingOptions: ParsingOptions): AMLConfiguration =
    super.withParsingOptions(parsingOptions).asInstanceOf[AMLConfiguration]

  override def withResourceLoader(rl: ResourceLoader): AMLConfiguration =
    super.withResourceLoader(rl).asInstanceOf[AMLConfiguration]

  override def withResourceLoaders(rl: List[ResourceLoader]): AMLConfiguration =
    super.withResourceLoaders(rl).asInstanceOf[AMLConfiguration]

  override def withUnitCache(cache: UnitCache): AMFGraphConfiguration =
    super.withUnitCache(cache).asInstanceOf[AMLConfiguration]

  override def withPlugin(amfPlugin: AMFParsePlugin): AMLConfiguration =
    super.withPlugin(amfPlugin).asInstanceOf[AMLConfiguration]

  override def withPlugins(plugins: List[AMFPlugin[_]]): AMLConfiguration =
    super.withPlugins(plugins).asInstanceOf[AMLConfiguration]

  override def withValidationProfile(profile: ValidationProfile): AMLConfiguration =
    super.withValidationProfile(profile).asInstanceOf[AMLConfiguration]

  override def createClient(): AMLClient = new AMLClient(this)
  // forInstnace ==  colecta dialects dinamicos
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
    withPlugin(new AMLInstancePlugin(dialect)).asInstanceOf[AMLConfiguration] //.withConstraints() //build contraints
  }

  def withCustomProfile(instancePath: String): Future[AMLConfiguration] = {
    createClient().parse(instancePath: String).map {
      case AMFResult(i: DialectInstance, _) => throw new UnsupportedOperationException() // SET REGISTRY PROFILE
      case _                                => this
    }
  }

  def forInstance(d: DialectInstanceUnit) = throw new UnsupportedOperationException()
}

private[amf] object AMLConfiguration {
  private val environment: AMFGraphConfiguration = AMFGraphConfiguration.predefined()

  /**
    * Predefined env to deal with AML documents based on
    * Predefined AMF Environment {@link amf.client.remod.AMFGraphConfiguration.predefined()}
    *
    * @return
    */
  def AML(): AMLConfiguration = {

    new AMLConfiguration(
        environment.resolvers,
        environment.errorHandlerProvider,
        environment.registry,
        environment.logger,
        environment.listeners,
        environment.options).withPlugins(List(AMLParsePlugin, AMFGraphParsePlugin)).asInstanceOf[AMLConfiguration]
  }
}
