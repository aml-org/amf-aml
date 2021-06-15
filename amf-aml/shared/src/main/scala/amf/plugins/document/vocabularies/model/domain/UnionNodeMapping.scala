package amf.plugins.document.vocabularies.model.domain
import amf.core.client.scala.model.domain.{DomainElement, Linkable}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.internal.utils._
import amf.plugins.document.vocabularies.metamodel.domain.UnionNodeMappingModel
import org.yaml.model.YMap

case class UnionNodeMapping(fields: Fields, annotations: Annotations)
    extends DomainElement
    with Linkable
    with MergeableMapping
    with NodeWithDiscriminator[UnionNodeMappingModel.type]
    with NodeMappable[UnionNodeMappingModel.type] {

  override def meta: UnionNodeMappingModel.type = UnionNodeMappingModel

  override def linkCopy(): UnionNodeMapping = UnionNodeMapping().withId(id)
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement =
    UnionNodeMapping.apply
  override def componentId: String = "/" + name.value().urlComponentEncoded
}

object UnionNodeMapping {
  def apply(): UnionNodeMapping = apply(Annotations())

  def apply(ast: YMap): UnionNodeMapping = apply(Annotations(ast))

  def apply(annotations: Annotations): UnionNodeMapping = UnionNodeMapping(Fields(), annotations)
}
