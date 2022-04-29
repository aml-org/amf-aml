package amf.rdf.client.platform

import amf.rdf.client.scala.{
  Literal => InternalLiteral,
  Node => InternalNode,
  PropertyObject => InternalPropertyObject,
  Uri => InternalUri
}
import amf.rdf.internal.convert.RdfClientConverter._

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class PropertyObject private[amf] (private[amf] val internal: InternalPropertyObject) {
  def value: String = internal.value
}

@JSExportAll
case class Literal private[amf] (private[amf] override val internal: InternalLiteral) extends PropertyObject(internal) {
  def literalType: ClientOption[String] = internal.literalType.asClient
}

@JSExportAll
case class Uri private[amf] (private[amf] override val internal: InternalUri) extends PropertyObject(internal) {}

@JSExportAll
case class Node private[amf] (private[amf] val internal: InternalNode) {

  def subject: String             = internal.subject
  def classes: ClientList[String] = internal.classes.asClient

  // It doesn't return a ClientOption[ClientList[PropertyObject]] because we don't have converters for nested types and None
  def getProperties(iri: String): ClientList[PropertyObject] =
    internal.getProperties(iri).getOrElse(Seq.empty).asClient

  def getKeys(): ClientList[String] = internal.getKeys().asClient
}
