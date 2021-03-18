package amf.client.environment

import amf.client.remod.{AMFEnvironment, AmfResult, ErrorHandlerProvider}
import amf.client.remod.amfcore.config.{AMFConfig, AMFOptions, AMFResolvers}
import amf.client.remod.amfcore.registry.AMFRegistry
import amf.plugins.document.graph.AMFGraphParsePlugin
import amf.plugins.document.vocabularies.{AMLInstancePlugin, AMLParsePlugin}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class AMLEnvironment(override val resolvers: AMFResolvers,
                          override val errorHandlerProvider: ErrorHandlerProvider,
                          override val registry: AMFRegistry,
                          override val amfConfig: AMFConfig,
                          override val options: AMFOptions)
    extends AMFEnvironment(resolvers, errorHandlerProvider, registry, amfConfig, options) {

  /**
    *
    * @param path
    * @return
    */
  def withDialect(path: String): Future[AMLEnvironment] = {
    getInstance().parse(path).map {
      // TODO We should also resolve the dialect
      case AmfResult(d: Dialect, _) => withDialect(d)
      case _                        => this
    }
  }

  def withDialect(dialect: Dialect): AMLEnvironment = {
    // TODO check if this dialect is resolved
    withPlugin(new AMLInstancePlugin(dialect)).withConstraints() //build contraints
  }

  def withoutDialect(dialect: Dialect): AMLEnvironment =
    pluginFor(dialect)
      .map(withoutPlugin)
      .getOrElse(this)
      .asInstanceOf[AMLEnvironment]

  private[amf] def pluginFor(dialect: Dialect): Option[AMLInstancePlugin] = {
    registry.plugins.allPlugins
      .find {
        case e: AMLInstancePlugin => e.dialect == dialect
        case _                    => false
      }
      .asInstanceOf[Option[AMLInstancePlugin]]
  }

  def withCustomProfile(instancePath: String): Future[AMLEnvironment] = {
    getInstance().parse(instancePath: String).map {
      case AmfResult(i: DialectInstance, _) => // SET REGISTRY PROFILE
      case _                                => this
    }
  }
}

private[amf] object AMLEnvironment {
  private val environment: AMFEnvironment = AMFEnvironment.predefined()

  def aml(): AMLEnvironment = {

    new AMLEnvironment(environment.resolvers,
                       environment.errorHandlerProvider,
                       environment.registry,
                       environment.amfConfig,
                       environment.options).withPlugins(List(AMLParsePlugin, AMFGraphParsePlugin))
  }

}
