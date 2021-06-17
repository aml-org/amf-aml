package amf.aml.client.platform.model.document

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.aml.client.platform.model.domain.{External, NodeMapping}
import amf.core.client.platform.model.document.{BaseUnit, EncodesModel}
import amf.aml.client.scala.model.document.{DialectFragment => InternalDialectFragment}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class DialectFragment(private[amf] val _internal: InternalDialectFragment) extends BaseUnit with EncodesModel {

  @JSExportTopLevel("DialectFragment")
  def this() = this(InternalDialectFragment())

  override def encodes: NodeMapping   = _internal.encodes
  def externals: ClientList[External] = _internal.externals.asClient

  def withExternals(externals: ClientList[External]): DialectFragment = {
    _internal.withExternals(externals.asInternal)
    this
  }

  def withEncodes(nodeMapping: NodeMapping): DialectFragment = {
    _internal.withEncodes(nodeMapping._internal)
    this
  }
}
