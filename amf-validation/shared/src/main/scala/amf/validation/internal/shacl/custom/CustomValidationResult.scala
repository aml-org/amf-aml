package amf.validation.internal.shacl.custom

import amf.core.internal.validation.core.ValidationResult

case class CustomValidationResult(
    message: Option[String],
    path: String,
    sourceConstraintComponent: String,
    focusNode: String,
    severity: String,
    sourceShape: String
) extends ValidationResult
