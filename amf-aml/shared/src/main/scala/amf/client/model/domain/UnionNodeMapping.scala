package amf.client.model.domain

import amf.client.model.StrField
import amf.client.convert.VocabulariesClientConverter._
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.plugins.document.vocabularies.model.domain.{UnionNodeMapping => InternalUnionNodeMapping}

@JSExportAll
case class UnionNodeMapping(override private[amf] val _internal: InternalUnionNodeMapping)
    extends DomainElement
    with Linkable {

  @JSExportTopLevel("model.domain.UnionNodeMapping")
  def this() = this(InternalUnionNodeMapping())

  def name: StrField = _internal.name

  def withName(name: String): UnionNodeMapping = {
    _internal.withName(name)
    this
  }
  def typeDiscriminatorName(): StrField      = _internal.typeDiscriminatorName()
  def typeDiscriminator(): ClientMap[String] = _internal.typeDiscriminator().asClient

  override def linkCopy(): UnionNodeMapping = UnionNodeMapping(_internal.linkCopy())
  def objectRange(): ClientList[StrField]   = _internal.objectRange().asClient
  def withObjectRange(range: ClientList[String]): UnionNodeMapping = {
    _internal.withObjectRange(range.asInternal)
    this
  }
  def withTypeDiscriminatorName(name: String): UnionNodeMapping = {
    _internal.withTypeDiscriminatorName(name)
    this
  }
  def withTypeDiscriminator(typesMapping: ClientMap[String]): UnionNodeMapping = {
    _internal.withTypeDiscriminator(typesMapping.asInternal)
    this
  }
}
