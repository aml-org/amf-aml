package amf.aml.internal.metamodel.domain

import amf.aml.client.scala.model.domain.UnionNodeMapping
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain._

object UnionNodeMappingModel extends AnyMappingModel with NodeWithDiscriminatorModel {

  override val fields
    : List[Field] = Name :: TypeDiscriminator :: TypeDiscriminatorName :: ObjectRange :: LinkableElementModel.fields ++ DomainElementModel.fields ++ AnyMappingModel.fields

  override val `type`
    : List[ValueType] = Namespace.Meta + "UnionNodeMapping" :: Namespace.Shacl + "Shape" :: DomainElementModel.`type`

  override def modelInstance: AmfObject = UnionNodeMapping()
}
