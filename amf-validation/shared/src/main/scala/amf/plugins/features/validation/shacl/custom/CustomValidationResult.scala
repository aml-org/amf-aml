package amf.plugins.features.validation.shacl.custom

import amf.core.validation.core.ValidationResult

case class CustomValidationResult(message: Option[String],
                                  path: String,
                                  sourceConstraintComponent: String,
                                  focusNode: String,
                                  severity: String,
                                  sourceShape: String)
    extends ValidationResult
