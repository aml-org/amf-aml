package amf.client.environment

import amf.client.remod._
import amf.core.validation.core.ValidationProfile
import amf.plugins.document.vocabularies.custom.ParsedValidationProfile
import amf.plugins.document.vocabularies.metamodel.document.{DialectInstanceModel, DialectModel, VocabularyModel}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, Vocabulary}
import amf.plugins.document.vocabularies.model.domain.DialectDomainElement

import scala.concurrent.{ExecutionContext, Future}

/** Contains common AML operations. Handles typed results. */
class AMLClient private[amf] (protected override val configuration: AMLConfiguration)
    extends AMFGraphClient(configuration) {

  override implicit val exec: ExecutionContext = configuration.resolvers.executionContext.executionContext

  override def getConfiguration: AMLConfiguration = configuration

  /**
    * parse a [[Dialect]]
    * @param url of the resource to parse
    * @return a Future [[AMLDialectResult]]
    */
  def parseDialect(url: String): Future[AMLDialectResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: Dialect, r) => new AMLDialectResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException.forMeta(other.bu.meta, DialectModel)
  }

  /**
    * parse a [[DialectInstance]]
    * @param url of the resource to parse
    * @return a Future [[AMLDialectInstanceResult]]
    */
  def parseDialectInstance(url: String): Future[AMLDialectInstanceResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: DialectInstance, r) => new AMLDialectInstanceResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException.forMeta(other.bu.meta, DialectInstanceModel)
  }

  /**
    * parse a {@link amf.plugins.document.vocabularies.model.document.DialectInstance}
    * @param url of the resource to parse
    * @return a Future {@link amf.client.environment.AMLDialectInstanceResult}
    */
  def parseValidationProfile(url: String): Future[ValidationProfile] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: DialectInstance, _) => parseValidationProfile(d)
    case other =>
      throw InvalidBaseUnitTypeException.forMeta(other.bu.meta, DialectInstanceModel)
  }

  def parseValidationProfile(dialect: DialectInstance): ValidationProfile = {
    if (dialect.definedBy().is(configuration.PROFILE_DIALECT_URL)) {
      ParsedValidationProfile(dialect.encodes.asInstanceOf[DialectDomainElement])
    } else
      throw InvalidBaseUnitTypeException(dialect.definedBy().value(), configuration.PROFILE_DIALECT_URL)
  }

  /**
    * parse a [[Vocabulary]]
    * @param url of the resource to parse
    * @return a Future [[AMLVocabularyResult]]
    */
  def parseVocabulary(url: String): Future[AMLVocabularyResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: Vocabulary, r) => new AMLVocabularyResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException.forMeta(other.bu.meta, VocabularyModel)
  }
}
