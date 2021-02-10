package amf.client.`new`

import amf.client.remote.Content
import amf.internal.resource.ResourceLoader
import amf.plugins.document.vocabularies.model.document.Dialect

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AmlEnvironment(override val resolvers: AmfResolvers,
                     override val errorHandlerProvider: ErrorHandlerProvider,
                     override val registry: AmfRegistry,
                     override val amfConfig: AmfConfig,
                     override val options: AmfOptions) extends AmfEnvironment(resolvers, errorHandlerProvider, registry, amfConfig, options){
  def withDialect(url:String):Future[AmfEnvironment] = {
    AmfParser.parse(url,this).map { r =>
      r.bu match {
        case d:Dialect =>
          this.withPlugin(new AmlParsePlugin(d)).withConstraintSet(new Rules(d))
        case _ => this //??
      }
    }
  }

}

object AmlEnvironment {

  def apply() = {
    val environment = AmfEnvironment.default()
    new AmlEnvironment(environment.resolvers,environment.errorHandlerProvider, environment.registry.withPlugin(AmlMetaParsePlugin),environment.amfConfig, environment.options)
  }



}
