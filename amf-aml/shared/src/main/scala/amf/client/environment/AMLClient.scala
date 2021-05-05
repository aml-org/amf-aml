package amf.client.environment

import amf.client.remod._
import amf.plugins.document.vocabularies.metamodel.document.{DialectInstanceModel, DialectModel, VocabularyModel}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, Vocabulary}

import scala.concurrent.{ExecutionContext, Future}

/**
  * AML Client object
  * @param configuration {@link amf.client.environment.AMLConfiguration}
  */
class AMLClient private[amf] (protected override val configuration: AMLConfiguration)
    extends AMFGraphClient(configuration) {

  override implicit val exec: ExecutionContext = configuration.resolvers.executionContext.executionContext

  override def getConfiguration: AMLConfiguration = configuration

  /**
    * parse a {@link amf.plugins.document.vocabularies.model.document.Dialect}
    * @param url of the resource to parse
    * @return a Future {@link amf.client.environment.AMLDialectResult}
    */
  def parseDialect(url: String): Future[AMLDialectResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: Dialect, r) => new AMLDialectResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException.forMeta(other.bu.meta, DialectModel)
  }

  /**
    * parse a {@link amf.plugins.document.vocabularies.model.document.DialectInstance}
    * @param url of the resource to parse
    * @return a Future {@link amf.client.environment.AMLDialectInstanceResult}
    */
  def parseDialectInstance(url: String): Future[AMLDialectInstanceResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: DialectInstance, r) => new AMLDialectInstanceResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException.forMeta(other.bu.meta, DialectInstanceModel)
  }

  /**
    * parse a {@link amf.plugins.document.vocabularies.model.document.Vocabulary}
    * @param url of the resource to parse
    * @return a Future {@link amf.client.environment.AMLVocabularyResult}
    */
  def parseVocabulary(url: String): Future[AMLVocabularyResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: Vocabulary, r) => new AMLVocabularyResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException.forMeta(other.bu.meta, VocabularyModel)
  }
}
