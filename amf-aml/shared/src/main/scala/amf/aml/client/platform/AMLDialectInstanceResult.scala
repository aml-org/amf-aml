package amf.aml.client.platform

import amf.aml.client.platform.model.document.DialectInstance
import amf.aml.client.scala.{AMLDialectInstanceResult => InternalAMLDialectInstanceResult}
import amf.core.client.platform.AMFResult
import amf.aml.internal.convert.VocabulariesClientConverter._
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class AMLDialectInstanceResult(private[amf] override val _internal: InternalAMLDialectInstanceResult)
    extends AMFResult(_internal) {
  def dialectInstance: DialectInstance = _internal.dialectInstance
}
