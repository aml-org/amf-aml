package amf.aml.client.platform.model.domain

import amf.aml.client.scala.model.domain.{DialectDomainElement => InternalDialectDomainElement}
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.domain.{DomainElement, Linkable}
import amf.core.client.platform.model.{BoolField, StrField}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
@JSExportAll
case class DialectDomainElement(override private[amf] val _internal: InternalDialectDomainElement)
    extends DomainElement
    with Linkable {

  @JSExportTopLevel("DialectDomainElement")
  def this() = this(InternalDialectDomainElement())

  def getObjectByProperty(uri: String): ClientList[DialectDomainElement] = _internal.getObjectByProperty(uri).asClient

  def isAbstract(): BoolField = _internal.isAbstract

  def withAbstract(isAbstract: Boolean): DialectDomainElement = {
    _internal.withAbstract(isAbstract)
    this
  }

  def declarationName: StrField = _internal.declarationName

  def withDeclarationName(name: String): DialectDomainElement = {
    _internal.withDeclarationName(name)
    this
  }

  def withInstanceTypes(types: ClientList[String]): DialectDomainElement = {
    _internal.withInstanceTypes(types.asInternal)
    this
  }

  def withDefinedby(nodeMapping: NodeMapping): DialectDomainElement = {
    _internal.withDefinedBy(nodeMapping._internal)
    this
  }

  def definedBy(): NodeMapping = NodeMapping(_internal.definedBy)

  def localRefName(): String = _internal.localRefName

  def includeName(): String = _internal.includeName

  def containsProperty(property: PropertyMapping): Boolean = _internal.containsProperty(property)

  def withObjectProperty(uri: String, value: DialectDomainElement): this.type = {
    _internal.withObjectProperty(uri, value)
    this
  }

  def withObjectCollectionProperty(propertyId: String, value: ClientList[DialectDomainElement]): this.type = {
    _internal.withObjectCollectionProperty(propertyId, value.asInternal)
    this
  }

  def getTypeUris(): ClientList[String] = _internal.meta.`type`.map(_.iri()).asClient

  def getPropertyUris(): ClientList[String] = _internal.meta.fields.map(_.value.iri()).asClient

  override def linkCopy(): DialectDomainElement = _internal.linkCopy()

  def withLiteralProperty(propertyId: String, value: String): this.type = {
    _internal.withLiteralProperty(propertyId, value)
    this
  }

  def withLiteralProperty(propertyId: String, value: Int): this.type = {
    _internal.withLiteralProperty(propertyId, value)
    this
  }

  def withLiteralProperty(propertyId: String, value: Double): this.type = {
    _internal.withLiteralProperty(propertyId, value)
    this
  }

  def withLiteralProperty(propertyId: String, value: Float): this.type = {
    _internal.withLiteralProperty(propertyId, value)
    this
  }

  def withLiteralProperty(propertyId: String, value: Boolean): this.type = {
    _internal.withLiteralProperty(propertyId, value)
    this
  }

  def withLiteralProperty(propertyId: String, value: ClientList[Any]): this.type = {
    _internal.withLiteralProperty(propertyId, value.asInternal.toList)
    this
  }
}
