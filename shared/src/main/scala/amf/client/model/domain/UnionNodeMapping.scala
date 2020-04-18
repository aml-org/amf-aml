package amf.client.model.domain

import amf.client.model.StrField
import amf.client.convert.VocabulariesClientConverter._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.plugins.document.vocabularies.model.domain.{UnionNodeMapping => InternalUnionNodeMapping}

@JSExportAll
case class UnionNodeMapping(override private[amf] val _internal: InternalUnionNodeMapping) extends DomainElement {

  @JSExportTopLevel("model.domain.UnionNodeMapping")
  def this() = this(InternalUnionNodeMapping())

  def name: StrField                                   = _internal.name

  def withName(name: String): UnionNodeMapping = {
    _internal.withName(name)
    this
  }
}
