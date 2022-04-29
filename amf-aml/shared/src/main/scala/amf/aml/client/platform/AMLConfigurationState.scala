package amf.aml.client.platform

import amf.aml.client.platform.model.document.Dialect
import amf.aml.client.platform.model.domain.SemanticExtension
import amf.aml.client.scala.{AMLConfigurationState => InternalAMLConfigurationState}
import amf.aml.internal.convert.VocabulariesClientConverter._
import scala.scalajs.js.annotation.JSExportAll

/* Contains methods to get information about the current state of the configuration */
@JSExportAll
class AMLConfigurationState private[amf] (private val _internal: InternalAMLConfigurationState) {

  private[amf] def this(configuration: AMLConfiguration) = {
    this(new InternalAMLConfigurationState(configuration._internal))
  }

  /** Get all instances of registered dialects
    * @return
    *   a list of [[Dialect]]
    */
  def getDialects(): ClientList[Dialect] = _internal.getDialects().asClient

  /** Find an instance of registered dialect with the provided name
    * @param name
    *   of the dialect to find
    * @return
    *   a list with the different versions of [[Dialect]] with the same name
    */
  def getDialect(name: String): ClientList[Dialect] = _internal.getDialect(name).asClient

  /** Find an instance of registered dialect with the provided name and version
    * @param name
    *   of the dialect to find
    * @param version
    *   of dialect to find
    * @return
    *   an optional [[Dialect]]
    */
  def getDialect(name: String, version: String): ClientOption[Dialect] = getDialect(name, version)

  /** Get all instances of SemanticExtensions present in the registered dialects
    * @return
    *   a list of [[SemanticExtension]]
    */
  def getExtensions(): ClientList[SemanticExtension] = _internal.getExtensions().asClient
}
