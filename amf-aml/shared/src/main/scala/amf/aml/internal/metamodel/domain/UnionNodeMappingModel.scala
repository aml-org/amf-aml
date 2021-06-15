package amf.aml.internal.metamodel.domain
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Iri, SortedArray}
import amf.core.internal.metamodel.domain._
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.domain.UnionNodeMapping

object UnionNodeMappingModel
    extends DomainElementModel
    with LinkableElementModel
    with MergeableMappingModel
    with NodeWithDiscriminatorModel
    with NodeMappableModel {

  override val fields
    : List[Field] = Name :: TypeDiscriminator :: TypeDiscriminatorName :: ObjectRange :: LinkableElementModel.fields ++ DomainElementModel.fields

  override val `type`
    : List[ValueType] = Namespace.Meta + "UnionNodeMapping" :: Namespace.Shacl + "Shape" :: DomainElementModel.`type`

  override def modelInstance: AmfObject = UnionNodeMapping()
}
