package amf.client.model.document

import amf.client.convert.VocabulariesClientConverter._
import amf.client.model.domain.{External, NodeMapping}
import amf.core.client.platform.model.document.{BaseUnit, DeclaresModel}
import amf.plugins.document.vocabularies.model.document.{DialectLibrary => InternalDialectLibrary}
import amf.plugins.document.vocabularies.model.domain.{NodeMapping => InternalNodeMapping}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class DialectLibrary(private[amf] val _internal: InternalDialectLibrary) extends BaseUnit with DeclaresModel {

  @JSExportTopLevel("model.document.DialectLibrary")
  def this() = this(InternalDialectLibrary())

  def externals: ClientList[External] = _internal.externals.asClient
  def nodeMappings(): ClientList[NodeMapping] =
    _internal.declares.collect({ case m: InternalNodeMapping => m }).asClient

  def withExternals(externals: ClientList[External]): DialectLibrary = {
    _internal.withExternals(externals.asInternal)
    this
  }

  def withNodeMappings(nodeMappings: ClientList[NodeMapping]): DialectLibrary = {
    _internal.withDeclares(nodeMappings.asInternal)
    this
  }
}
