package amf.aml.client.scala.model.domain

import amf.core.client.scala.model.domain.{DataNode, DomainElement}
import amf.core.internal.metamodel.domain.ShapeModel

trait WithDefaultFacet extends DomainElement {

  private[amf] def default(): Option[DataNode] = Option(fields.field(ShapeModel.Default))
  private[amf] def withDefault(node: DataNode) = set(ShapeModel.Default, node)
}
