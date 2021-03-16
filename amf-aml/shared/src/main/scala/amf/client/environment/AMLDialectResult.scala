package amf.client.environment

import amf.client.remod.AMFResult
import amf.core.validation.AMFValidationReport
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, Vocabulary}

class AMLDialectResult(val dialect: Dialect, report: AMFValidationReport) extends AMFResult(dialect, report)

class AMLDialectInstanceResult(val dialectInstance: DialectInstance, report: AMFValidationReport)
    extends AMFResult(dialectInstance, report)

class AMLVocabularyResult(val vocabulary: Vocabulary, report: AMFValidationReport)
    extends AMFResult(vocabulary, report)
