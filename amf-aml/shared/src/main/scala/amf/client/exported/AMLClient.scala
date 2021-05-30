package amf.client.exported

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.environment.{AMLClient => InternalAMLClient}
import amf.client.convert.VocabulariesClientConverter._
import amf.client.model.document.DialectInstance
import amf.core.validation.core.ValidationProfile

import scala.concurrent.ExecutionContext

@JSExportAll
class AMLClient private[amf] (private val _internal: InternalAMLClient) extends AMFGraphClient(_internal) {

  private implicit val ec: ExecutionContext = _internal.getConfiguration.getExecutionContext

  @JSExportTopLevel("AMLClient")
  def this(configuration: AMLConfiguration) = {
    this(new InternalAMLClient(configuration))
  }

  override def getConfiguration: AMLConfiguration = _internal.getConfiguration

  /**
    * parse a {@link amf.plugins.document.vocabularies.model.document.Dialect}
    * @param url of the resource to parse
    * @return a Future {@link amf.client.environment.AMLDialectResult}
    */
  def parseDialect(url: String): ClientFuture[AMLDialectResult] = _internal.parseDialect(url).asClient

  /**
    * parse a {@link amf.plugins.document.vocabularies.model.document.DialectInstance}
    * @param url of the resource to parse
    * @return a Future {@link amf.client.environment.AMLDialectInstanceResult}
    */
  def parseDialectInstance(url: String): ClientFuture[AMLDialectInstanceResult] =
    _internal.parseDialectInstance(url).asClient

  // TODO ARM: export and addjava doc
//  def parseValidationProfile(url: String): ClientFuture[ValidationProfile] =
//    _internal.parseValidationProfile(url).asClient
//
//  // TODO ARM: export and addjava doc
//  def parseValidationProfile(dialect: DialectInstance): ClientFuture[ValidationProfile] =
//    _internal.parseValidationProfile(dialect).asClient

  /**
    * parse a {@link amf.plugins.document.vocabularies.model.document.Vocabulary}
    * @param url of the resource to parse
    * @return a Future {@link amf.client.environment.AMLVocabularyResult}
    */
  def parseVocabulary(url: String): ClientFuture[AMLVocabularyResult] = _internal.parseVocabulary(url).asClient
}
