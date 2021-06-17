package amf.aml.client.platform.model.domain

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.StrField
import amf.core.client.platform.model.domain.DomainElement
import amf.aml.client.scala.model.domain.{External => InternalExternal}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class External(override private[amf] val _internal: InternalExternal) extends DomainElement {

  @JSExportTopLevel("External")
  def this() = this(InternalExternal())

  def alias: StrField = _internal.alias
  def base: StrField  = _internal.base

  def withAlias(alias: String): External = _internal.withAlias(alias)
  def withBase(base: String): External   = _internal.withBase(base)
}
