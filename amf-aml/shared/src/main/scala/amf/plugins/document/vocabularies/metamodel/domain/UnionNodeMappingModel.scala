package amf.plugins.document.vocabularies.metamodel.domain
import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Iri, SortedArray}
import amf.core.metamodel.domain._
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.UnionNodeMapping

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
