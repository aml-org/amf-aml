package amf.client.exported

import amf.client.environment.{AMLDialectInstanceResult => InternalAMLDialectInstanceResult}
import amf.client.model.document.DialectInstance
import amf.client.convert.VocabulariesClientConverter._
import amf.client.interface.AMFResult

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class AMLDialectInstanceResult(private[amf] override val _internal: InternalAMLDialectInstanceResult)
    extends AMFResult(_internal) {
  def dialectInstance: DialectInstance = _internal.dialectInstance
}
