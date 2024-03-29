package amf.aml.client.platform.model.document

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.StrField
import amf.aml.client.platform.model.domain.{DialectDomainElement, External}
import amf.core.client.platform.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.aml.client.scala.model.document.{DialectInstance => InternalDialectInstance}
import amf.aml.client.scala.model.domain.{DialectDomainElement => InternalDialectDomainElement}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class DialectInstance(private[amf] val _internal: InternalDialectInstance)
    extends BaseUnit
    with EncodesModel
    with DeclaresModel
    with DialectInstanceUnit {

  @JSExportTopLevel("DialectInstance")
  def this() = this(InternalDialectInstance())

  def externals: ClientList[External] = _internal.externals.asClient

  override def encodes: DialectDomainElement =
    DialectDomainElement(_internal.encodes.asInstanceOf[InternalDialectDomainElement])

  def withEncodes(encoded: DialectDomainElement): DialectInstance = {
    _internal.withEncodes(encoded._internal)
    this
  }

  def withExternals(externals: ClientList[External]): DialectInstance = {
    _internal.withExternals(externals.asInternal)
    this
  }
}
