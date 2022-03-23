package amf.aml.client.platform.model.domain

import amf.aml.client.scala.model.domain.{AnyMapping => InternalAnyMapping}
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.StrField
import amf.core.client.platform.model.domain.DomainElement

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
abstract class AnyMapping(override private[amf] val _internal: InternalAnyMapping) extends DomainElement {

  def and(): ClientList[StrField]        = _internal.and.asClient
  def or(): ClientList[StrField]         = _internal.or.asClient
  def components(): ClientList[StrField] = _internal.components.asClient

  def withAnd(andMappings: ClientList[String]): AnyMapping = {
    _internal.withAnd(andMappings.asInternal)
    this
  }
  def withOr(orMappings: ClientList[String]): AnyMapping = {
    _internal.withOr(orMappings.asInternal)
    this
  }
  def withComponents(components: ClientList[String]): AnyMapping = {
    _internal.withOr(components.asInternal)
    this
  }
}
