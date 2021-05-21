package amf.client.environment

import amf.client.remod._
import amf.plugins.document.vocabularies.metamodel.document.{DialectInstanceModel, DialectModel, VocabularyModel}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, Vocabulary}

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
