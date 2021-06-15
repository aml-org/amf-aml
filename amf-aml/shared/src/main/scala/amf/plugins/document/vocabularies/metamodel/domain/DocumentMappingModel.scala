package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri, Str}
import amf.core.internal.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.DocumentMapping

object DocumentMappingModel extends DomainElementModel {

  val DocumentName: Field = Field(
      Str,
      Namespace.Core + "name",
      ModelDoc(ModelVocabularies.Core, "name", "Name of the document for a dialect base unit"))

  val EncodedNode: Field = Field(
      Iri,
      Namespace.Meta + "encodedNode",
      ModelDoc(ModelVocabularies.Meta, "encodedNode", "Node in the dialect encoded in the target mapped base unit"))

  val DeclaredNodes: Field = Field(
      Array(PublicNodeMappingModel),
      Namespace.Meta + "declaredNode",
      ModelDoc(ModelVocabularies.Meta, "declaredNode", "Node in the dialect declared in the target mappend base unit")
  )

  override def fields: List[Field] = DocumentName :: EncodedNode :: DeclaredNodes :: DomainElementModel.fields

  override def modelInstance: AmfObject = DocumentMapping()

  override val `type`: List[ValueType] = Namespace.Meta + "DocumentMapping" :: DomainElementModel.`type`

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Meta,
      "DocumentMapping",
      "Mapping for a particular dialect document into a graph base unit"
  )
}
