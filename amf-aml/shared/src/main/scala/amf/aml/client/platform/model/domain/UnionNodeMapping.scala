package amf.aml.client.platform.model.domain

import amf.aml.client.scala.model.domain.{UnionNodeMapping => InternalUnionNodeMapping}
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.StrField
import amf.core.client.platform.model.domain.Linkable

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class UnionNodeMapping(override private[amf] val _internal: InternalUnionNodeMapping)
    extends AnyMapping(_internal)
    with Linkable {

  @JSExportTopLevel("UnionNodeMapping")
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
