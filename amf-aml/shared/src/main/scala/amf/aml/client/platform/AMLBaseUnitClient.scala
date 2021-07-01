package amf.aml.client.platform

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.aml.client.scala.{AMLBaseUnitClient => InternalAMLBaseUnitClient}
import amf.aml.internal.convert.VocabulariesClientConverter._

/** Contains common AML operations. Handles typed results. */
@JSExportAll
class AMLBaseUnitClient private[amf] (private val _internal: InternalAMLBaseUnitClient)
    extends BaseAMLBaseUnitClient(_internal) {

  private[amf] def this(configuration: AMLConfiguration) = {
    this(new InternalAMLBaseUnitClient(configuration))
  }

  override def getConfiguration(): AMLConfiguration = _internal.getConfiguration
}
