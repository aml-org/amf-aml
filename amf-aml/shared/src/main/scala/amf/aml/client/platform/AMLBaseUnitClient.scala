package amf.aml.client.platform

import amf.aml.client.scala.{AMLBaseUnitClient => InternalAMLBaseUnitClient}
import amf.aml.internal.convert.VocabulariesClientConverter._

import scala.scalajs.js.annotation.JSExportAll

/** Contains common AML operations. Handles typed results. */
@JSExportAll
class AMLBaseUnitClient private[amf] (private val _internal: InternalAMLBaseUnitClient)
    extends BaseAMLBaseUnitClient(_internal) {

  private[amf] def this(configuration: AMLConfiguration) = {
    this(new InternalAMLBaseUnitClient(configuration))
  }

  override def getConfiguration(): AMLConfiguration = _internal.getConfiguration
}
