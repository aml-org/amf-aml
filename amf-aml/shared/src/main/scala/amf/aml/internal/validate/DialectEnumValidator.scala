package amf.aml.internal.validate

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.DialectDomainElement
import amf.aml.internal.parse.instances.parser.LiteralValueParser
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.core.client.common.validation.{AMFStyle, AmlProfile, ProfileName}
import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.core.client.scala.model.domain.AmfScalar
import amf.core.client.scala.model.{BoolField, DoubleField, FloatField, IntField, StrField, ValueField}
import amf.core.client.scala.validation.AMFValidationReport
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.plugins.syntax.SyamlAMFErrorHandler
import amf.core.internal.validation.ShaclReportAdaptation
import amf.core.internal.validation.core.{ValidationReport, ValidationSpecification}
import amf.validation.internal.shacl.custom.{CustomShaclValidator, ScalarElementExtractor}
import org.yaml.model.{YNode, YScalar}

case class DialectEnumValidator() extends ShaclReportAdaptation {

  def validate(dialect: Dialect): AMFValidationReport = {
    val candidates = LiteralCandidateCollector.collect(dialect)
    val reports = candidates.map { candidate =>
      val eh =
        new SyamlAMFErrorHandler(
            DefaultErrorHandler()
        ) // create error handler for each candidate to avoid duplicating errors in report
      validateCandidate(dialect, eh, candidate)
    }
    val mergedReport = mergeReports(dialect, reports)
    mergedReport
  }

  private def validateCandidate(dialect: Dialect, eh: SyamlAMFErrorHandler, candidate: AmlValidationCandidate) = {
    val validations =
      new AMFDialectValidations(dialect)(DefaultNodeMappableFinder(dialect))
        .propertyValidations(candidate.node)
        .filter(
            _.propertyConstraints.exists(_.ramlPropertyId == candidate.mapping.nodePropertyMapping().value())
        ) // should optimize this
    val nodes = candidate.enums.flatMap(toNode)
    parseErrors(eh, candidate, nodes)
    val reports = nodes.map { case (_, scalar) =>
      val report = validate(scalar, candidate, validations)
      adaptToAmfReport(
          dialect,
          AmlProfile,
          report,
          scalar.annotations.location(),
          LexicalInformation(scalar.annotations.lexical())
      )
    }
    val mergedReport: AMFValidationReport = mergeReports(dialect, reports)
    mergedReport.copy(results = mergedReport.results ++ eh.getResults)
  }

  private def mergeReports(dialect: Dialect, reports: Seq[AMFValidationReport]) = {
    val mergedReport = reports.foldLeft(AMFValidationReport.empty(dialect.id, AmlProfile)) { (acc, curr) =>
      acc.merge(curr)
    }
    mergedReport
  }

  private def validate(
      scalar: AmfScalar,
      candidate: AmlValidationCandidate,
      validations: Seq[ValidationSpecification]
  ): ValidationReport = {
    val validator = new CustomShaclValidator(Map.empty, AMFStyle, new ScalarElementExtractor(scalar))
    validator.validateProperties(DialectDomainElement("anId", candidate.node, Annotations()), validations)
  }

  private def parseErrors(
      eh: SyamlAMFErrorHandler,
      candidate: AmlValidationCandidate,
      nodes: List[(YNode, AmfScalar)]
  ) = {
    nodes.foreach { case (node, scalar) =>
      LiteralValueParser.parseLiteralValue(node, candidate.mapping, "anId", scalar.annotations)(eh)
    }
  }

  private def toNode(value: Any): Option[(YNode, AmfScalar)] = value match {
    case value @ AmfScalar(inner: Boolean, _) => Some(YNode(inner), value)
    case value @ AmfScalar(inner: Int, _)     => Some(YNode(inner), value)
    case value @ AmfScalar(inner: String, _)  => Some(YNode(inner), value)
    case value @ AmfScalar(inner: Float, _)   => Some(YNode(inner), value)
    case value @ AmfScalar(inner: Double, _)  => Some(YNode(inner), value)
    case _                                    => None
  }
}
