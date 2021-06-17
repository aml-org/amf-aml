package amf.aml.client.platform.model.document

import amf.aml.client.platform.model.domain.DialectDomainElement
import amf.core.client.platform.model.document.{BaseUnit, EncodesModel}
import amf.aml.client.scala.model.document.{DialectInstanceFragment => InternalDialectInstanceFragment}
import amf.aml.client.scala.model.domain.{DialectDomainElement => InternalDialectDomainElement}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class DialectInstanceFragment(private[amf] val _internal: InternalDialectInstanceFragment)
    extends BaseUnit
    with EncodesModel {

  @JSExportTopLevel("DialectInstanceFragment")
  def this() = this(InternalDialectInstanceFragment())

  override def encodes: DialectDomainElement =
    DialectDomainElement(_internal.encodes.asInstanceOf[InternalDialectDomainElement])

  def withEncodes(encoded: DialectDomainElement): DialectInstanceFragment = {
    _internal.withEncodes(encoded._internal)
    this
  }
}
