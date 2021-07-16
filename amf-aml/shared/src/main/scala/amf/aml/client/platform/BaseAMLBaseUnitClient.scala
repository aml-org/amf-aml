package amf.aml.client.platform

import amf.aml.client.platform.model.document.DialectInstance
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.common.validation.ValidationProfile
import amf.core.client.platform.AMFGraphBaseUnitClient
import amf.aml.client.scala.{AMLBaseUnitClient => InternalAMLClient}

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.JSExportAll

/** Contains common AML document operations. Handles typed results. */
@JSExportAll
abstract class BaseAMLBaseUnitClient private[amf] (private val _internal: InternalAMLClient)
    extends AMFGraphBaseUnitClient(_internal) {

  protected implicit val ec: ExecutionContext = _internal.getConfiguration.getExecutionContext

  /**
    * parse a [[amf.aml.client.scala.model.document.Dialect]]
    *
    * @param url of the resource to parse
    * @return a CompletableFuture [[AMLDialectResult]]
    */
  def parseDialect(url: String): ClientFuture[AMLDialectResult] = _internal.parseDialect(url).asClient

  /**
    * parse a [[amf.aml.client.scala.model.document.DialectInstance]]
    *
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
    * parse a [[amf.aml.client.scala.model.document.Vocabulary]]
    *
    * @param url of the resource to parse
    * @return a CompletableFuture [[AMLVocabularyResult]]
    */
  def parseVocabulary(url: String): ClientFuture[AMLVocabularyResult] = _internal.parseVocabulary(url).asClient
}
