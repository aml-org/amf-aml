package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri, Str}
import amf.core.internal.metamodel.domain.{DomainElementModel, ExternalModelVocabularies, ModelDoc, ModelVocabularies}
import amf.core.client.scala.vocabulary.Namespace

abstract class PropertyTermModel extends DomainElementModel {
  val Name: Field =
    Field(Str, Namespace.Core + "name", ModelDoc(ModelVocabularies.Core, "name", "Name of the property term"))

  val DisplayName: Field = Field(
      Str,
      Namespace.Core + "displayName",
      ModelDoc(ModelVocabularies.Core, "displayName", "Human readable name for the property term"))

  val Description: Field = Field(
      Str,
      Namespace.Core + "description",
      ModelDoc(ModelVocabularies.Core, "description", "Human readable description of the property term"))

  val Range: Field = Field(
      Iri,
      Namespace.Rdfs + "range",
      ModelDoc(ExternalModelVocabularies.Rdfs, "range", "Range of the proeprty term, scalar or object"))

  val SubPropertyOf: Field = Field(
      Array(Iri),
      Namespace.Rdfs + "subPropertyOf",
      ModelDoc(ExternalModelVocabularies.Rdfs, "subPropertyOf", "Subsumption relationship for terms"))

  override def fields: List[Field] = DisplayName :: Description :: Range :: SubPropertyOf :: DomainElementModel.fields
}
