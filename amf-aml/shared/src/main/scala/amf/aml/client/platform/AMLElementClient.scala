package amf.aml.client.platform

import amf.aml.client.platform.render.AmlDomainElementEmitter
import amf.aml.client.scala.{AMLElementClient => InternalAMLElementClient}
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.domain.DomainElement
import amf.core.client.platform.render.AMFElementRenderer
import org.yaml.builder.DocBuilder

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class AMLElementClient private[amf] (private val _internal: InternalAMLElementClient)
    extends BaseAMLElementClient(_internal) {

  private[amf] def this(configuration: AMLConfiguration) = {
    this(new InternalAMLElementClient(configuration))
  }

  /**
    * Currently supports rendering of dialect domain elements
    *
    * @param references : optional parameter which will improve emission of references defined in element
    */
  override def renderToBuilder[T](element: DomainElement, builder: DocBuilder[T]): Unit = {
    AmlDomainElementEmitter.emitToBuilder(element, getConfiguration(), builder)
  }

  override def getConfiguration(): AMLConfiguration = _internal.getConfiguration
}
