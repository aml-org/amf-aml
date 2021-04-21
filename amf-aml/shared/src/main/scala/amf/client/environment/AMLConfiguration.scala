package amf.client.environment

import amf.client.parse.DefaultParserErrorHandler
import amf.client.remod.amfcore.config._
import amf.client.remod.amfcore.plugins.AMFPlugin
import amf.client.remod.amfcore.registry.AMFRegistry
import amf.client.remod.parsing.{AMLDialectInstanceParsingPlugin, AMLDialectParsingPlugin, AMLVocabularyParsingPlugin}
import amf.client.remod.rendering.{
  AMLDialectInstanceRenderingPlugin,
  AMLDialectRenderingPlugin,
  AMLVocabularyRenderingPlugin
}
import amf.client.remod.{AMFGraphConfiguration, AMFResult, ErrorHandlerProvider}
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.ValidationProfile
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.internal.reference.UnitCache
import amf.internal.resource.ResourceLoader
import amf.plugins.document.graph.{AMFGraphParsePlugin, AMFGraphRenderPlugin}
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, DialectInstanceUnit}
import scala.concurrent.ExecutionContext.Implicits.global // TODO should this be here?
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

  override def withPlugin(amfPlugin: AMFPlugin[_]): AMLConfiguration =
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
    AMLPlugin.registry.register(dialect)
    this
  }

  def withCustomProfile(instancePath: String): Future[AMLConfiguration] = {
    createClient().parse(instancePath: String).map {
      case AMFResult(i: DialectInstance, _) => throw new UnsupportedOperationException() // SET REGISTRY PROFILE
      case _                                => this
    }
  }

  def forInstance(d: DialectInstanceUnit) = throw new UnsupportedOperationException()
}

private[amf] object AMLConfiguration extends PlatformSecrets {

  /**
    * Predefined env to deal with AML documents based on
    * Predefined AMF Environment {@link amf.client.remod.AMFGraphConfiguration.predefined()}
    *
    * @return
    */
  def predefined(): AMLConfiguration = {
    val predefinedGraphConfiguration: AMFGraphConfiguration = AMFGraphConfiguration.predefined()

    val predefinedPlugins = new AMLDialectParsingPlugin() ::
      new AMLVocabularyParsingPlugin() ::
      new AMLDialectRenderingPlugin() ::
      new AMLVocabularyRenderingPlugin() ::
      AMFGraphParsePlugin ::
      AMFGraphRenderPlugin ::
      Nil

    new AMLConfiguration(
        predefinedGraphConfiguration.resolvers,
        predefinedGraphConfiguration.errorHandlerProvider,
        predefinedGraphConfiguration.registry,
        predefinedGraphConfiguration.logger,
        predefinedGraphConfiguration.listeners,
        predefinedGraphConfiguration.options
    ).withPlugins(predefinedPlugins)
  }

  // TODO: what about nested $dialect references?
  def forInstance(url: String, mediaType: Option[String] = None): Future[AMLConfiguration] = {
    var env      = predefined()
    val ctx      = new CompilerContextBuilder(url, platform, eh = DefaultParserErrorHandler.withRun()).build()
    val compiler = new AMFCompiler(ctx, mediaType, None)
    for {
      content                <- compiler.fetchContent()
      eitherContentOrAst     <- Future.successful(compiler.parseSyntax(content))
      root                   <- Future.successful(eitherContentOrAst.right.get) if eitherContentOrAst.isRight
      plugin                 <- Future.successful(compiler.getDomainPluginFor(root))
      documentWithReferences <- compiler.parseReferences(root, plugin.get) if plugin.isDefined
    } yield {
      documentWithReferences.references.foreach { r =>
        r.unit match {
          case d: Dialect =>
            val parsing: AMLDialectInstanceParsingPlugin     = new AMLDialectInstanceParsingPlugin(d)
            val rendering: AMLDialectInstanceRenderingPlugin = new AMLDialectInstanceRenderingPlugin(d)
            env = env.withPlugins(List(parsing, rendering))
          case _ => // Ignore
        }
      }
      env
    }
  }
}
