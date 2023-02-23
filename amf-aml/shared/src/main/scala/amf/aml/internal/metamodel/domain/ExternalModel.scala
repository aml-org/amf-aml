package amf.aml.internal.metamodel.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Str
import amf.core.internal.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.domain.External

object ExternalModel extends DomainElementModel {

  val DisplayName: Field = Field(
    Str,
    Namespace.Core + "displayName",
    ModelDoc(ModelVocabularies.Core, "displayName", "The display name of the item")
  )
  val Base: Field =
    Field(Str, Namespace.Meta + "base", ModelDoc(ModelVocabularies.Meta, "base", "Base URI for the external model"))

  override def modelInstance: AmfObject = External()

  override def fields: List[Field] = DisplayName :: Base :: DomainElementModel.fields

  override val `type`: List[ValueType] =
    Namespace.Owl + "Ontology" :: Namespace.Meta + "ExternalVocabulary" :: DomainElementModel.`type`
}
