package amf.plugins.features.validation.shacl.custom

import amf.{MessageStyle, OASStyle, RAMLStyle}
import amf.core.services.ShaclValidationOptions
import amf.core.validation.SeverityLevels
import amf.core.validation.core.{
  PropertyConstraint,
  ShaclSeverityUris,
  ValidationReport,
  ValidationResult,
  ValidationSpecification
}

import scala.collection.mutable

object CustomValidationReport {
  def empty = new CustomValidationReport(Nil)
}

class ReportBuilder(options: ShaclValidationOptions) {

  def build(): CustomValidationReport = CustomValidationReport(results.toList)

  private val duplicates: mutable.Set[String] = mutable.Set()

  private val results: mutable.ListBuffer[ValidationResult] = mutable.ListBuffer.empty

  def reportFailure(validationSpecification: ValidationSpecification, id: String): Unit = {
    reportFailure(validationSpecification, id, "")
  }

  def reportFailure(validationSpecification: ValidationSpecification,
                    propertyConstraint: PropertyConstraint,
                    id: String): Unit = {
    reportFailure(validationSpecification, id, propertyConstraint.ramlPropertyId)
  }

  def reportFailure(validationSpecification: ValidationSpecification,
                    id: String,
                    propertyPath: Option[String] = None): Unit = {
    reportFailure(validationSpecification, id, propertyPath.getOrElse(""))
  }

  def reportFailure(validationSpec: ValidationSpecification, id: String, path: String): Unit = {
    registerResult(
        CustomValidationResult(
            message = getMessageOf(validationSpec, options.messageStyle),
            path = path,
            sourceConstraintComponent = validationSpec.id,
            focusNode = id,
            severity = ShaclSeverityUris.amfSeverity(validationSpec.severity),
            sourceShape = validationSpec.id
        ))
  }

  private def registerResult(result: ValidationResult): Unit = {
    val key = result.sourceShape + result.sourceConstraintComponent + result.focusNode
    if (!duplicates.contains(key)) {
      duplicates += key
      results.append(result)
    }
  }

  private def getMessageOf(validationSpec: ValidationSpecification, style: MessageStyle): Option[String] =
    style match {
      case RAMLStyle => validationSpec.ramlMessage.orElse(Some(validationSpec.message))
      case OASStyle  => validationSpec.oasMessage.orElse(Some(validationSpec.message))
      case _         => Some(validationSpec.message)
    }
}

case class CustomValidationReport(var rs: List[ValidationResult] = Nil) extends ValidationReport {

  override def conforms: Boolean = results.exists(_.severity == SeverityLevels.VIOLATION)

  override def results: List[ValidationResult] = rs
}
