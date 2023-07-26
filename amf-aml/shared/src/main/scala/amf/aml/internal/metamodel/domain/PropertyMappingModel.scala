package amf.aml.internal.metamodel.domain

import amf.aml.client.scala.model.domain.PropertyMapping
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies, ShapeModel}

object PropertyMappingModel
    extends DomainElementModel
    with PropertyLikeMappingModel
    with MergeableMappingModel
    with NodeWithDiscriminatorModel {

  override def fields: List[Field] =
    NodePropertyMapping :: Name :: LiteralRange :: ObjectRange ::
      MapKeyProperty :: MapValueProperty :: MapTermKeyProperty :: MapTermValueProperty ::
      MinCount :: Pattern :: Minimum :: Maximum :: AllowMultiple :: Sorted :: Enum :: TypeDiscriminator ::
      Unique :: ExternallyLinkable :: Mandatory :: TypeDiscriminatorName :: MergePolicy :: ShapeModel.Default :: DomainElementModel.fields

  override def modelInstance: AmfObject = PropertyMapping()

  override val `type`: List[ValueType] =
    Namespace.Meta + "NodePropertyMapping" :: /* Namespace.Shacl + "PropertyShape" :: */ DomainElementModel.`type`

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.Meta,
    "NodePropertyMapping",
    "Semantic mapping from an input AST in a dialect document to the output graph of information for a class of output node"
  )
}
