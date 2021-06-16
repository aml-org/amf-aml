package amf.aml.client.platform

import amf.aml.client.platform.model.document.Dialect
import amf.core.client.platform.AMFResult
import amf.aml.client.scala.{AMLDialectResult => InternalAMLDialectResult}
import scala.scalajs.js.annotation.JSExportAll
import amf.aml.internal.convert.VocabulariesClientConverter._

@JSExportAll
class AMLDialectResult(private[amf] override val _internal: InternalAMLDialectResult) extends AMFResult(_internal) {
  def dialect: Dialect = _internal.dialect
}
