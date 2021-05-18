package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Any, Bool, Double, Int, Iri, SortedArray, Str}
import amf.core.metamodel.domain.{DomainElementModel, ExternalModelVocabularies, ModelDoc, ModelVocabularies}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.PropertyMapping

object PropertyMappingModel
    extends DomainElementModel
    with PropertyLikeMappingModel
    with MergeableMappingModel
    with NodeWithDiscriminatorModel {

  val MapKeyProperty: Field = Field(
      Str,
      Namespace.Meta + "mapProperty",
      ModelDoc(ModelVocabularies.Meta,
               "mapLabelProperty",
               "Marks the mapping as a 'map' mapping syntax. Directly related with mapTermKeyProperty")
  )

  val MapValueProperty: Field = Field(
      Str,
      Namespace.Meta + "mapValueProperty",
      ModelDoc(ModelVocabularies.Meta,
               "mapLabelValueProperty",
               "Marks the mapping as a 'map value' mapping syntax. Directly related with mapTermValueProperty")
  )

  val MapTermKeyProperty: Field = Field(
      Iri,
      Namespace.Meta + "mapTermProperty",
      ModelDoc(ModelVocabularies.Meta, "mapTermPropertyUri", "Marks the mapping as a 'map' mapping syntax. "))

  val MapTermValueProperty: Field = Field(
      Iri,
      Namespace.Meta + "mapTermValueProperty",
      ModelDoc(ModelVocabularies.Meta, "mapTermValueProperty", "Marks the mapping as a 'map value' mapping syntax")
  )

  override def fields: List[Field] =
    NodePropertyMapping :: Name :: LiteralRange :: ObjectRange ::
      MapKeyProperty :: MapValueProperty :: MapTermKeyProperty :: MapTermValueProperty ::
      MinCount :: Pattern :: Minimum :: Maximum :: AllowMultiple :: Sorted :: Enum :: TypeDiscriminator ::
      Unique :: ExternallyLinkable :: TypeDiscriminatorName :: MergePolicy :: DomainElementModel.fields

  override def modelInstance: AmfObject = PropertyMapping()

  override val `type`
    : List[ValueType] = Namespace.Meta + "NodePropertyMapping" :: /* Namespace.Shacl + "PropertyShape" :: */ DomainElementModel.`type`

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Meta,
      "NodePropertyMapping",
      "Semantic mapping from an input AST in a dialect document to the output graph of information for a class of output node"
  )
}
