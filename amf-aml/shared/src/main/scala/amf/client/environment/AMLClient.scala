package amf.client.environment

import amf.client.remod._
import amf.plugins.document.vocabularies.metamodel.document.{DialectInstanceModel, DialectModel, VocabularyModel}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, Vocabulary}

import scala.concurrent.{ExecutionContext, Future}

// TODO: ARM remove private[amf]
private[amf] class AMLClient(protected override val configuration: AMLConfiguration) extends AMFClient(configuration) {

  override implicit val exec: ExecutionContext = configuration.resolvers.executionContext.executionContext

  override def getConfiguration: AMLConfiguration = configuration
  def parseDialect(url: String): Future[AMLDialectResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: Dialect, r) => new AMLDialectResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException.forMeta(other.bu.meta, DialectModel)
  }

  def parseDialectInstance(url: String): Future[AMLDialectInstanceResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: DialectInstance, r) => new AMLDialectInstanceResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException.forMeta(other.bu.meta, DialectInstanceModel)
  }

  def parseVocabulary(url: String): Future[AMLVocabularyResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: Vocabulary, r) => new AMLVocabularyResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException.forMeta(other.bu.meta, VocabularyModel)
  }
}
