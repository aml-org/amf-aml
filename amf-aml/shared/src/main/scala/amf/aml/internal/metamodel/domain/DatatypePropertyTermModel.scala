package amf.aml.internal.metamodel.domain

import amf.core.internal.metamodel.domain.DomainElementModel
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.domain.DatatypePropertyTerm

object DatatypePropertyTermModel extends PropertyTermModel {
  override val `type`: List[ValueType] =
    Namespace.Owl + "DatatypeProperty" :: Namespace.Meta + "Property" :: DomainElementModel.`type`
  override def modelInstance: AmfObject = DatatypePropertyTerm()
}
