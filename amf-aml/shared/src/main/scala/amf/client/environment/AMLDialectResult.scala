package amf.client.environment

import amf.core.client.scala.AMFResult
import amf.core.client.scala.validation.AMFValidationResult
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, Vocabulary}

class AMLDialectResult(val dialect: Dialect, results: Seq[AMFValidationResult]) extends AMFResult(dialect, results)

class AMLDialectInstanceResult(val dialectInstance: DialectInstance, results: Seq[AMFValidationResult])
    extends AMFResult(dialectInstance, results)

class AMLVocabularyResult(val vocabulary: Vocabulary, results: Seq[AMFValidationResult])
    extends AMFResult(vocabulary, results)
