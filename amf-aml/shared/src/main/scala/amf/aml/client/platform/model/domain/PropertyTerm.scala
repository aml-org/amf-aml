package amf.aml.client.platform.model.domain

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.domain.DomainElement
import amf.core.client.platform.model.StrField
import amf.aml.client.scala.model.domain.{
  DatatypePropertyTerm => InternalDatatypePropertyTerm,
  ObjectPropertyTerm => InternalObjectPropertyTerm,
  PropertyTerm => InternalPropertyTerm
}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
abstract class PropertyTerm(override private[amf] val _internal: InternalPropertyTerm) extends DomainElement {

  def name: StrField                      = _internal.name
  def displayName: StrField               = _internal.displayName
  def description: StrField               = _internal.description
  def range: StrField                     = _internal.range
  def subPropertyOf: ClientList[StrField] = _internal.subPropertyOf.asClient

  def withName(name: String): PropertyTerm = {
    _internal.withName(name)
    this
  }
  def withDisplayName(displayName: String): PropertyTerm = {
    _internal.withDisplayName(displayName)
    this
  }
  def withDescription(description: String): PropertyTerm = {
    _internal.withDescription(description)
    this
  }
  def withRange(range: String): PropertyTerm = {
    _internal.withRange(range)
    this
  }

  def withSubClasOf(superProperties: ClientList[String]): PropertyTerm = {
    _internal.withSubClassOf(superProperties.asInternal)
    this
  }
}

/**
  * Object property term from a vocabulary
  * @param _internal
  */
@JSExportAll
case class ObjectPropertyTerm(override private[amf] val _internal: InternalObjectPropertyTerm)
    extends PropertyTerm(_internal) {

  @JSExportTopLevel("ObjectPropertyTerm")
  def this() = this(InternalObjectPropertyTerm())

}

/**
  * Datatype property term from a vocabulary
  * @param _internal
  */
@JSExportAll
case class DatatypePropertyTerm(override private[amf] val _internal: InternalDatatypePropertyTerm)
    extends PropertyTerm(_internal) {

  @JSExportTopLevel("DatatypePropertyTerm")
  def this() = this(InternalDatatypePropertyTerm())

}
