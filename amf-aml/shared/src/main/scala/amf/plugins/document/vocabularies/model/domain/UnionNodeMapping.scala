package amf.plugins.document.vocabularies.model.domain
import amf.core.model.domain.{DomainElement, Linkable}
import amf.core.parser.{Annotations, Fields}
import amf.core.utils._
import amf.plugins.document.vocabularies.metamodel.domain.UnionNodeMappingModel
import org.yaml.model.YMap

case class UnionNodeMapping(fields: Fields, annotations: Annotations)
    extends DomainElement
    with Linkable
    with MergeableMapping
    with NodeWithDiscriminator[UnionNodeMappingModel.type]
    with NodeMappable {

  override def withName(name: String): UnionNodeMapping = super.withName(name).asInstanceOf[UnionNodeMapping]

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
