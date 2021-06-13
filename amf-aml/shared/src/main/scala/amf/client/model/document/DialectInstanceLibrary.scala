package amf.client.model.document

import amf.core.client.platform.model.document.{BaseUnit, DeclaresModel}
import amf.plugins.document.vocabularies.model.document.{DialectInstanceLibrary => InternalDialectInstanceLibrary}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class DialectInstanceLibrary(private[amf] val _internal: InternalDialectInstanceLibrary)
    extends BaseUnit
    with DeclaresModel {

  @JSExportTopLevel("model.document.DialectInstanceLibrary")
  def this() = this(InternalDialectInstanceLibrary())
}
