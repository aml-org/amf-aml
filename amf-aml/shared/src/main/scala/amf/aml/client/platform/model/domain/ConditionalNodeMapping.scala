package amf.aml.client.platform.model.domain

import amf.core.client.platform.model.StrField
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.domain.{DomainElement, Linkable}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.aml.client.scala.model.domain.{ConditionalNodeMapping => InternalConditionalNodeMapping}

@JSExportAll
case class ConditionalNodeMapping(override private[amf] val _internal: InternalConditionalNodeMapping)
    extends DomainElement
    with Linkable {

  @JSExportTopLevel("ConditionalNodeMapping")
  def this() = this(InternalConditionalNodeMapping())

  def name: StrField = _internal.name

  def withName(name: String): ConditionalNodeMapping = {
    _internal.withName(name)
    this
  }

  def ifMapping(): StrField   = _internal.ifMapping
  def thenMapping(): StrField = _internal.thenMapping
  def elseMapping(): StrField = _internal.elseMapping

  override def linkCopy(): ConditionalNodeMapping = ConditionalNodeMapping(_internal.linkCopy())

  def withIfMapping(ifMapping: String): ConditionalNodeMapping = {
    _internal.withIfMapping(ifMapping)
    this
  }
  def withThenMapping(thenMapping: String): ConditionalNodeMapping = {
    _internal.withIfMapping(thenMapping)
    this
  }
  def withElseMapping(elseMapping: String): ConditionalNodeMapping = {
    _internal.withIfMapping(elseMapping)
    this
  }
}
