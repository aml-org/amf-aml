package amf.aml.client.platform.model.document

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.StrField
import amf.aml.client.platform.model.domain.{DialectDomainElement, External}
import amf.core.client.platform.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.aml.client.scala.model.document.{DialectInstancePatch => InternalPatchInstance}
import amf.aml.client.scala.model.domain.{DialectDomainElement => InternalDialectDomainElement}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
@JSExportAll
class DialectInstancePatch(private[amf] val _internal: InternalPatchInstance)
    extends BaseUnit
    with EncodesModel
    with DeclaresModel
    with DialectInstanceUnit {

  @JSExportTopLevel("DialectInstancePatch")
  def this() = this(InternalPatchInstance())

  def externals: ClientList[External] = _internal.externals.asClient

  override def encodes: DialectDomainElement =
    DialectDomainElement(_internal.encodes.asInstanceOf[InternalDialectDomainElement])

  def withEncodes(encoded: DialectDomainElement): DialectInstancePatch = {
    _internal.withEncodes(encoded._internal)
    this
  }

  def withExternals(externals: ClientList[External]): DialectInstancePatch = {
    _internal.withExternals(externals.asInternal)
    this
  }
}
