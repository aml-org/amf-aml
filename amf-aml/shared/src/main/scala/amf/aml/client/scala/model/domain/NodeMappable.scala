package amf.aml.client.scala.model.domain
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.{DomainElement, Linkable}
import amf.aml.internal.metamodel.domain.NodeMappableModel

trait NodeMappable[+M <: NodeMappableModel] extends DomainElement with Linkable {
  def name: StrField                    = fields.field(meta.Name)
  def withName(name: String): this.type = set(meta.Name, name)

  override def meta: M
}

object NodeMappable {
  type AnyNodeMappable = NodeMappable[_ <: NodeMappableModel]
}
