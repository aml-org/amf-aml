package amf.aml.client.platform.model.domain

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.StrField
import amf.core.client.platform.model.domain.DomainElement
import amf.aml.client.scala.model.domain.{PublicNodeMapping => InternalPublicNodeMapping}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class PublicNodeMapping(override private[amf] val _internal: InternalPublicNodeMapping) extends DomainElement {

  @JSExportTopLevel("PublicNodeMapping")
  def this() = this(InternalPublicNodeMapping())

  def name(): StrField = _internal.name()
  def withName(name: String): PublicNodeMapping = {
    _internal.withName(name)
    this
  }
  def mappedNode(): StrField = _internal.mappedNode()
  def withMappedNode(mappedNode: String): PublicNodeMapping = {
    _internal.withMappedNode(mappedNode)
    this
  }
}
