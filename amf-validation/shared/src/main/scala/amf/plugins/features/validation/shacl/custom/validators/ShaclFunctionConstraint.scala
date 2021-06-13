package amf.plugins.features.validation.shacl.custom.validators

import amf.core.client.scala.model.domain.AmfObject
import amf.core.internal.validation.core.ValidationSpecification
import amf.plugins.features.validation.shacl.custom.CustomShaclValidator.{
  CustomShaclFunction,
  CustomShaclFunctions,
  PropertyInfo
}
import amf.plugins.features.validation.shacl.custom.{ConstraintValidator, ReportBuilder}

case class ShaclFunctionConstraint(customFunctions: CustomShaclFunctions) extends ConstraintValidator {
  override def canValidate(spec: ValidationSpecification): Boolean = spec.functionConstraint.isDefined

  override def validate(spec: ValidationSpecification, element: AmfObject, reportBuilder: ReportBuilder): Unit = {
    spec.functionConstraint.foreach { functionConstraint =>
      functionConstraint.internalFunction.foreach(name => {
        val validationFunction = getFunctionForName(name)
        // depending if propertyInfo is provided, violation is thrown at a given property, or by default on element
        val onViolation = (propertyInfo: Option[PropertyInfo]) =>
          propertyInfo match {
            case Some((_, field)) => reportBuilder.reportFailure(spec, element.id, Some(field.toString))
            case _                => reportBuilder.reportFailure(spec, element.id)
        }
        validationFunction(element, onViolation)
      })
    }
  }

  private def getFunctionForName(name: String): CustomShaclFunction =
    customFunctions.getOrElse(
        name,
        throw new Exception(s"Custom function validations not supported in customm SHACL validator: $name"))
}
