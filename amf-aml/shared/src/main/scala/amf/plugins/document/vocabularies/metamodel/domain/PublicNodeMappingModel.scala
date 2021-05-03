package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Iri, Str}
import amf.core.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.PublicNodeMapping

object PublicNodeMappingModel extends DomainElementModel {

  val Name: Field =
    Field(Str, Namespace.Core + "name", ModelDoc(ModelVocabularies.Core, "name", "Name of the mapping"))

  val MappedNode: Field = Field(
      Iri,
      Namespace.Meta + "mappedNode",
      ModelDoc(ModelVocabularies.Meta, "mappedNode", "Node in the dialect definition associated to this mapping"))

  override def fields: List[Field] = Name :: MappedNode :: DomainElementModel.fields

  override def modelInstance: AmfObject = PublicNodeMapping()

  override val `type`: List[ValueType] = Namespace.Meta + "PublicNodeMapping" :: DomainElementModel.`type`

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Meta,
      "PublicNodeMapping",
      "Mapping for a graph node mapping to a particular function in a dialect"
  )
}