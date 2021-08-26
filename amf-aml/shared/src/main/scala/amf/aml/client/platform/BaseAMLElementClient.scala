package amf.aml.client.platform

import amf.aml.client.scala.{AMLElementClient => InternalAMLClient}
import amf.core.client.platform.AMFGraphElementClient
import amf.core.client.platform.model.domain.DomainElement
import org.yaml.builder.DocBuilder

import scala.scalajs.js.annotation.JSExportAll

/** Contains common AML operations not related to document. */
@JSExportAll
abstract class BaseAMLElementClient private[amf] (private val _internal: InternalAMLClient)
    extends AMFGraphElementClient(_internal) {

  /**
    * Currently supports rendering of dialect domain elements
    * @param references : optional parameter which will improve emission of references defined in element
    */
  def renderToBuilder[T](element: DomainElement, builder: DocBuilder[T]): Unit

}
