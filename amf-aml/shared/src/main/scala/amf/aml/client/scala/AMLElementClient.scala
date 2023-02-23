package amf.aml.client.scala

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.NodeMapping
import amf.aml.client.scala.render.AmlDomainElementEmitter
import amf.core.client.scala.AMFGraphElementClient
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.DomainElement
import org.yaml.model.YNode

/** Contains common AML operations not related to document. */
class AMLElementClient private[amf] (protected override val configuration: AMLConfiguration)
    extends AMFGraphElementClient(configuration) {

  override def getConfiguration: AMLConfiguration = configuration

  /** Currently supports rendering of dialect domain elements
    * @param references
    *   : optional parameter which will improve emission of references defined in element
    */
  def renderElement(element: DomainElement, references: Seq[BaseUnit] = Nil): YNode = {

    configuration
      .configurationState()
      .getDialects()
      .find(
        _.declares
          .collectFirst({ case nm: NodeMapping if element.meta.`type`.exists(_.iri() == nm.id) => nm })
          .isDefined
      )
      .map { d =>
        AmlDomainElementEmitter.emit(element, d, configuration.errorHandlerProvider.errorHandler(), references)
      }
      .getOrElse(YNode.Null)

  }
}
