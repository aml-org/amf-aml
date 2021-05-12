package amf.plugins.document.vocabularies.model.domain
import amf.core.model.StrField
import amf.core.model.domain.{DomainElement, Linkable}
import amf.plugins.document.vocabularies.metamodel.domain.NodeMappableModel

trait NodeMappable[+M <: NodeMappableModel] extends DomainElement with Linkable {
  def name: StrField                    = fields.field(meta.Name)
  def withName(name: String): this.type = set(meta.Name, name)

  override def meta: M
}

object NodeMappable {
  type AnyNodeMappable = NodeMappable[_ <: NodeMappableModel]
}