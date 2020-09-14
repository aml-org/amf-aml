package amf.plugins.features.validation

import amf.core.validation.SeverityLevels
import amf.core.validation.core.{ValidationReport, ValidationResult}

import scala.collection.mutable

class CustomValidationReport(var rs: List[ValidationResult] = Nil) extends ValidationReport {

  val duplicates: mutable.Set[String] = mutable.Set()

  override def conforms: Boolean = results.exists(_.severity == SeverityLevels.VIOLATION)

  override def results: List[ValidationResult] = rs

  def registerResult(result: ValidationResult): Unit = {
    val key = result.sourceShape + result.sourceConstraintComponent + result.focusNode
    if (!duplicates.contains(key)) {
      duplicates += key
      rs ++= Seq(result)
    }
  }
}
