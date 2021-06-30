package amf.aml.client.platform

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.aml.client.scala.{AMLDocumentClient => InternalAMLDocumentClient}
import amf.aml.internal.convert.VocabulariesClientConverter._

/** Contains common AML operations. Handles typed results. */
@JSExportAll
class AMLDocumentClient private[amf] (private val _internal: InternalAMLDocumentClient)
    extends BaseAMLDocumentClient(_internal) {

  private[amf] def this(configuration: AMLConfiguration) = {
    this(new InternalAMLDocumentClient(configuration))
  }

  override def getConfiguration(): AMLConfiguration = _internal.getConfiguration
}
