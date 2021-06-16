package amf.aml.client.platform.model.domain

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.domain.{DomainElement, Linkable}
import amf.core.client.scala.model.StrField
import amf.aml.client.scala.model.domain.{NodeMapping => InternalNodeMapping}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class NodeMapping(override private[amf] val _internal: InternalNodeMapping) extends DomainElement with Linkable {

  @JSExportTopLevel("model.domain.NodeMapping")
  def this() = this(InternalNodeMapping())

  def name: StrField                                   = _internal.name
  def nodetypeMapping: StrField                        = _internal.nodetypeMapping
  def propertiesMapping(): ClientList[PropertyMapping] = _internal.propertiesMapping().asClient
  def idTemplate: StrField                             = _internal.idTemplate
  def mergePolicy: StrField                            = _internal.mergePolicy

  def withName(name: String): NodeMapping = {
    _internal.withName(name)
    this
  }

  def withNodeTypeMapping(nodeType: String): NodeMapping = {
    _internal.withNodeTypeMapping(nodeType)
    this
  }

  def withPropertiesMapping(props: ClientList[PropertyMapping]): NodeMapping = {
    _internal.withPropertiesMapping(props.asInternal)
    this
  }

  def withIdTemplate(idTemplate: String): NodeMapping = {
    _internal.withIdTemplate(idTemplate)
    this
  }

  def withMergePolicy(mergePolicy: String): NodeMapping = {
    _internal.withMergePolicy(mergePolicy)
    this
  }

  override def linkCopy(): NodeMapping = _internal.linkCopy()
}
