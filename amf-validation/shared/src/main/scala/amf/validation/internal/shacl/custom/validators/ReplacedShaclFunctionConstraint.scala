package amf.validation.internal.shacl.custom.validators

import amf.core.client.scala.model.domain.AmfObject
import amf.core.internal.validation.core.{FunctionConstraint, ValidationSpecification}
import amf.validation.internal.shacl.custom.CustomShaclValidator.CustomShaclFunctions
import amf.validation.internal.shacl.custom.{ConstraintValidator, ReportBuilder}

case class ReplacedShaclFunctionConstraint(customFunctions: CustomShaclFunctions) extends ConstraintValidator {
  override def canValidate(spec: ValidationSpecification): Boolean = spec.replacesFunctionConstraint.isDefined

  override def validate(spec: ValidationSpecification, element: AmfObject, reportBuilder: ReportBuilder): Unit = {
    spec.replacesFunctionConstraint.foreach { functionName =>
      val nextSpecification = spec.copy(
          functionConstraint =
            Some(FunctionConstraint(message = Some(spec.message), internalFunction = Some(functionName)))
      )
      ShaclFunctionConstraint(customFunctions).validate(nextSpecification, element, reportBuilder)
    }
  }
}
