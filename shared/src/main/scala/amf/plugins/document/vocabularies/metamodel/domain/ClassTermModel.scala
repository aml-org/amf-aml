package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Array, Iri, Str}
import amf.core.metamodel.domain.{DomainElementModel, ExternalModelVocabularies, ModelDoc, ModelVocabularies}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.ClassTerm

object ClassTermModel extends DomainElementModel {

  val Name = Field(Str, Namespace.Core + "name", ModelDoc(ModelVocabularies.Core, "name", "Name of the ClassTerm"))
  val DisplayName = Field(Str,
                          Namespace.Core + "displayName",
                          ModelDoc(ModelVocabularies.Core, "displayName", "Human readable name for the term"))
  val Description = Field(Str,
                          Namespace.Core + "description",
                          ModelDoc(ModelVocabularies.Core, "description", "Human readable description for the term"))
  val Properties = Field(
      Array(Iri),
      Namespace.Meta + "properties",
      ModelDoc(ModelVocabularies.Meta, "properties", "Properties that have the ClassTerm in the domain"))
  val SubClassOf = Field(
      Array(Iri),
      Namespace.Rdfs + "subClassOf",
      ModelDoc(ExternalModelVocabularies.Rdfs, "subClassOf", "Subsumption relationship across terms"))

  override def modelInstance: AmfObject = ClassTerm()

  override def fields: List[Field] =
    Name :: DisplayName :: Description :: Properties :: SubClassOf :: DomainElementModel.fields

  override val `type`: List[ValueType] = Namespace.Owl + "Class" :: DomainElementModel.`type`
}
