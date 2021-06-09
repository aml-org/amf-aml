package amf.client.exported

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.environment.{AMLClient => InternalAMLClient}
import amf.client.convert.VocabulariesClientConverter._
import amf.client.model.document.DialectInstance
import amf.core.validation.core.ValidationProfile

import scala.concurrent.ExecutionContext

/** Contains common AML operations. Handles typed results. */
@JSExportAll
class AMLClient private[amf] (private val _internal: InternalAMLClient) extends BaseAMLClient(_internal) {

  @JSExportTopLevel("AMLClient")
  def this(configuration: AMLConfiguration) = {
    this(new InternalAMLClient(configuration))
  }

  override def getConfiguration(): AMLConfiguration = _internal.getConfiguration
}
