package amf.aml.internal.metamodel.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri, Str}
import amf.core.internal.metamodel.domain.{DomainElementModel, ExternalModelVocabularies, ModelDoc, ModelVocabularies}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.domain.ClassTerm

object ClassTermModel extends DomainElementModel {

  val Name: Field =
    Field(Str, Namespace.Core + "name", ModelDoc(ModelVocabularies.Core, "name", "Name of the ClassTerm"))

  val DisplayName: Field = Field(Str,
                                 Namespace.Core + "displayName",
                                 ModelDoc(ModelVocabularies.Core, "displayName", "Human readable name for the term"))
  val Description: Field = Field(
      Str,
      Namespace.Core + "description",
      ModelDoc(ModelVocabularies.Core, "description", "Human readable description for the term"))
  val Properties: Field = Field(
      Array(Iri),
      Namespace.Meta + "properties",
      ModelDoc(ModelVocabularies.Meta, "properties", "Properties that have the ClassTerm in the domain"))
  val SubClassOf: Field = Field(
      Array(Iri),
      Namespace.Rdfs + "subClassOf",
      ModelDoc(ExternalModelVocabularies.Rdfs, "subClassOf", "Subsumption relationship across terms"))

  override def modelInstance: AmfObject = ClassTerm()

  override def fields: List[Field] =
    Name :: DisplayName :: Description :: Properties :: SubClassOf :: DomainElementModel.fields

  override val `type`: List[ValueType] = Namespace.Owl + "Class" :: DomainElementModel.`type`
}
