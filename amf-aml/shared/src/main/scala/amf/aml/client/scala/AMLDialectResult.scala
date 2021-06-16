package amf.aml.client.scala

import amf.aml.client.scala.model.document.{Dialect, DialectInstance, Vocabulary}
import amf.core.client.scala.AMFResult
import amf.core.client.scala.validation.AMFValidationResult

class AMLDialectResult(val dialect: Dialect, results: Seq[AMFValidationResult]) extends AMFResult(dialect, results)

class AMLDialectInstanceResult(val dialectInstance: DialectInstance, results: Seq[AMFValidationResult])
    extends AMFResult(dialectInstance, results)

class AMLVocabularyResult(val vocabulary: Vocabulary, results: Seq[AMFValidationResult])
    extends AMFResult(vocabulary, results)
