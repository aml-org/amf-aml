package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.Str
import amf.core.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.External

object ExternalModel extends DomainElementModel {

  val DisplayName = Field(Str,
                          Namespace.Core + "displayName",
                          ModelDoc(ModelVocabularies.Core, "displayName", "The display name of the item"))
  val Base =
    Field(Str, Namespace.Meta + "base", ModelDoc(ModelVocabularies.Meta, "base", "Base URI for the external model"))

  override def modelInstance: AmfObject = External()

  override def fields: List[Field] = DisplayName :: Base :: DomainElementModel.fields

  override val `type`
    : List[ValueType] = Namespace.Owl + "Ontology" :: Namespace.Meta + "ExternalVocabulary" :: DomainElementModel.`type`
}
