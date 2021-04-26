package amf.plugins.features.validation.shacl.custom.validators

import amf.core.model.domain.{AmfArray, AmfObject, AmfScalar, DomainElement}
import amf.core.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.PropertyConstraintValidator.extractPropertyValue
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}

case object InConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.in.nonEmpty

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: AmfObject,
                        reportBuilder: ReportBuilder): Unit = {
    if (propertyConstraint.in.nonEmpty) validateIn(spec, propertyConstraint, parent, reportBuilder)
  }

  private def validateIn(validationSpecification: ValidationSpecification,
                         propertyConstraint: PropertyConstraint,
                         parentElement: AmfObject,
                         reportBuilder: ReportBuilder): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, _: AmfScalar, Some(value: String))) =>
        if (!propertyConstraint.in.contains(value)) {
          reportBuilder.reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case Some((_, arr: AmfArray, _)) =>
        arr.values.foreach {
          case scalar: AmfScalar =>
            if (!propertyConstraint.in.contains(scalar.value.asInstanceOf[String])) {
              reportBuilder.reportFailure(validationSpecification, propertyConstraint, parentElement.id)
            }
          case _ => // ignore
        }

      case _ => // ignore
    }
  }
}
