package amf.client.exported

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import amf.client.environment.{AMLClient => InternalAMLClient}
import amf.client.convert.VocabulariesClientConverter._
import amf.client.model.document.DialectInstance
import amf.client.validate.ValidationProfile

import scala.concurrent.ExecutionContext

/** Contains common AML operations. Handles typed results. */
@JSExportAll
abstract class BaseAMLClient private[amf] (private val _internal: InternalAMLClient)
    extends AMFGraphClient(_internal) {

  protected implicit val ec: ExecutionContext = _internal.getConfiguration.getExecutionContext

  /**
    * parse a [[amf.plugins.document.vocabularies.model.document.Dialect]]
    * @param url of the resource to parse
    * @return a CompletableFuture [[AMLDialectResult]]
    */
  def parseDialect(url: String): ClientFuture[AMLDialectResult] = _internal.parseDialect(url).asClient

  /**
    * parse a [[amf.plugins.document.vocabularies.model.document.DialectInstance]]
    * @param url of the resource to parse
    * @return a CompletableFuture [[AMLDialectInstanceResult]]
    */
  def parseDialectInstance(url: String): ClientFuture[AMLDialectInstanceResult] =
    _internal.parseDialectInstance(url).asClient

  /**
    * @param url of the validation profile to parse
    * @return a CompletableFuture [[ValidationProfile]]
    */
  def parseValidationProfile(url: String): ClientFuture[ValidationProfile] =
    _internal.parseValidationProfile(url).asClient

  /**
    * @param instance of the validation profile dialect to parse
    * @return a [[ValidationProfile]]
    */
  def parseValidationProfile(instance: DialectInstance): ValidationProfile =
    _internal.parseValidationProfile(instance)

  /**
    * parse a [[amf.plugins.document.vocabularies.model.document.Vocabulary]]
    * @param url of the resource to parse
    * @return a CompletableFuture [[AMLVocabularyResult]]
    */
  def parseVocabulary(url: String): ClientFuture[AMLVocabularyResult] = _internal.parseVocabulary(url).asClient
}
