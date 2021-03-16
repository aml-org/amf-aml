package amf.client.environment

import amf.client.remod._
import amf.plugins.document.vocabularies.metamodel.document.{DialectInstanceModel, DialectModel, VocabularyModel}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, Vocabulary}

import scala.concurrent.{ExecutionContext, Future}

class AMLClient(override val configuration: AMFConfiguration) extends AMFClient(configuration) {

  override implicit val exec: ExecutionContext = configuration.resolvers.executionContext.executionContext

  def parseDialect(url: String): Future[AMLDialectResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: Dialect, r) => new AMLDialectResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException(other.bu.meta.`type`.headOption.map(_.iri()).getOrElse(""),
                                         DialectModel.`type`.head.iri())
  }

  def parseDialectInstance(url: String): Future[AMLDialectInstanceResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: DialectInstance, r) => new AMLDialectInstanceResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException(other.bu.meta.`type`.headOption.map(_.iri()).getOrElse(""),
                                         DialectInstanceModel.`type`.head.iri())
  }

  def parseVocabulary(url: String): Future[AMLVocabularyResult] = AMFParser.parse(url, configuration).map {
    case AMFResult(d: Vocabulary, r) => new AMLVocabularyResult(d, r)
    case other =>
      throw InvalidBaseUnitTypeException(other.bu.meta.`type`.headOption.map(_.iri()).getOrElse(""),
                                         VocabularyModel.`type`.head.iri())
  }
}