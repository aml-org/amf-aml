package amf.aml.client.platform

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class AMLElementClient private[amf] (protected val configuration: AMLConfiguration)
    extends BaseAMLElementClient(configuration) {
  def getConfiguration(): AMLConfiguration = configuration
}
