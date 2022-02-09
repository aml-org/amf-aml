package amf.aml.client.scala

import amf.aml.client.scala.model.document.{Dialect, DialectInstance, Vocabulary}
import amf.core.client.scala.{AMFParseResult, AMFResult}
import amf.core.client.scala.validation.AMFValidationResult

private[amf] object AMLDialectResult {
  def unapply(result: AMLDialectResult): Option[(Dialect, Seq[AMFValidationResult])] =
    Some((result.dialect, result.results))
}

class AMLDialectResult(val dialect: Dialect, results: Seq[AMFValidationResult])
    extends AMFParseResult(dialect, results)

class AMLDialectInstanceResult(val dialectInstance: DialectInstance, results: Seq[AMFValidationResult])
    extends AMFParseResult(dialectInstance, results)

class AMLVocabularyResult(val vocabulary: Vocabulary, results: Seq[AMFValidationResult])
    extends AMFParseResult(vocabulary, results)
