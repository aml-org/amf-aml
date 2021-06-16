package amf.aml.internal.metamodel.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Str
import amf.core.internal.metamodel.domain.DomainElementModel
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.domain.VocabularyReference

object VocabularyReferenceModel extends DomainElementModel {
  val Alias: Field = Field(Str, Namespace.Document + "alias")

  val Reference: Field = Field(Str, Namespace.Document + "reference")

  val Base: Field = Field(Str, Namespace.Meta + "base")

  override def modelInstance: AmfObject = VocabularyReference()

  override val fields: List[Field] = Alias :: Reference :: Base :: DomainElementModel.fields

  override val `type`: List[ValueType] = Namespace.Meta + "VocabularyReference" :: DomainElementModel.`type`
}
