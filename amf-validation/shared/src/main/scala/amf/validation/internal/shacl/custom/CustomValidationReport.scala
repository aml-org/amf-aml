package amf.validation.internal.shacl.custom

import amf.core.client.common.validation.{MessageStyle, OASStyle, RAMLStyle, SeverityLevels}
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.validation.core._

import scala.collection.mutable

object CustomValidationReport {
  def empty = new CustomValidationReport(Nil)
}

class ReportBuilder(messageStyle: MessageStyle) {

  def build(): CustomValidationReport = CustomValidationReport(results.toList)

  private val duplicates: mutable.Set[String] = mutable.Set()

  private val results: mutable.ListBuffer[ValidationResult] = mutable.ListBuffer.empty

  def reportFailure(
      validationSpecification: ValidationSpecification,
      propertyConstraint: PropertyConstraint,
      id: String,
      position: Option[LexicalInformation],
      location: Option[String]
  ): Unit = {
    reportFailure(validationSpecification, id, propertyConstraint.ramlPropertyId, None, position, location)
  }

  def reportFailure(
      validationSpec: ValidationSpecification,
      id: String,
      path: String,
      customMessage: Option[String] = None,
      position: Option[LexicalInformation] = None,
      location: Option[String] = None
  ): Unit = {
    registerResult(
      CustomValidationResult(
        message = customMessage.orElse(getMessageOf(validationSpec, messageStyle)),
        path = path,
        sourceConstraintComponent = validationSpec.id,
        focusNode = id,
        severity = ShaclSeverityUris.amfSeverity(validationSpec.severity),
        sourceShape = validationSpec.id,
        position = position,
        location = location
      )
    )
  }

  private def registerResult(result: ValidationResult): Unit = {
    val key =
      result.sourceShape + result.sourceConstraintComponent + result.focusNode + result.message + result.position + result.location
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

  override def conforms: Boolean = !results.exists(_.severity == SeverityLevels.VIOLATION)

  override def results: List[ValidationResult] = rs
}
