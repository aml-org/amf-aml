package amf.client.exported

import amf.client.environment.{AMLDialectResult => InternalAMLDialectResult}
import amf.client.model.document.Dialect
import amf.client.convert.VocabulariesClientConverter._
import amf.client.interface.AMFResult

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class AMLDialectResult(private[amf] override val _internal: InternalAMLDialectResult) extends AMFResult(_internal) {
  def dialect: Dialect = _internal.dialect
}
