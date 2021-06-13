package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.internal.metamodel.domain.DomainElementModel
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.ObjectPropertyTerm

object ObjectPropertyTermModel extends PropertyTermModel {
  override val `type`
    : List[ValueType]                   = Namespace.Owl + "ObjectProperty" :: Namespace.Meta + "Property" :: DomainElementModel.`type`
  override def modelInstance: AmfObject = ObjectPropertyTerm()
}
