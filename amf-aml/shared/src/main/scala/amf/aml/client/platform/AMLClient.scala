package amf.aml.client.platform

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.aml.client.scala.{AMLClient => InternalAMLClient}
import amf.aml.internal.convert.VocabulariesClientConverter._
/** Contains common AML operations. Handles typed results. */
@JSExportAll
class AMLClient private[amf] (private val _internal: InternalAMLClient) extends BaseAMLClient(_internal) {

  @JSExportTopLevel("AMLClient")
  def this(configuration: AMLConfiguration) = {
    this(new InternalAMLClient(configuration))
  }

  override def getConfiguration(): AMLConfiguration = _internal.getConfiguration
}
