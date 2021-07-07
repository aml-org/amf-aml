package amf.aml.client.platform

import amf.aml.client.scala.{AMLElementClient => InternalAMLElementClient}
import amf.aml.internal.convert.VocabulariesClientConverter._

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class AMLElementClient private[amf] (private val _internal: InternalAMLElementClient)
    extends BaseAMLElementClient(_internal) {

  private[amf] def this(configuration: AMLConfiguration) = {
    this(new InternalAMLElementClient(configuration))
  }

  override def getConfiguration(): AMLConfiguration = _internal.getConfiguration

}
