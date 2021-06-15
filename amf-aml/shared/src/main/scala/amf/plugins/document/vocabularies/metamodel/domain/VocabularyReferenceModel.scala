package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Str
import amf.core.internal.metamodel.domain.DomainElementModel
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.VocabularyReference

object VocabularyReferenceModel extends DomainElementModel {
  val Alias: Field = Field(Str, Namespace.Document + "alias")

  val Reference: Field = Field(Str, Namespace.Document + "reference")

  val Base: Field = Field(Str, Namespace.Meta + "base")

  override def modelInstance: AmfObject = VocabularyReference()

  override val fields: List[Field] = Alias :: Reference :: Base :: DomainElementModel.fields

  override val `type`: List[ValueType] = Namespace.Meta + "VocabularyReference" :: DomainElementModel.`type`
}
