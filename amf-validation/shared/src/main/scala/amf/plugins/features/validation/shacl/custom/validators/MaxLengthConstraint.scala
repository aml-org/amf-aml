package amf.plugins.features.validation.shacl.custom.validators

import amf.core.model.domain.{AmfArray, AmfElement, AmfScalar, DomainElement}
import amf.core.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.PropertyConstraintValidator.extractPropertyValue
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}

case object MaxLengthConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.maxLength.isDefined

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: DomainElement,
                        reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.maxLength.foreach { maxLength =>
      extractPropertyValue(propertyConstraint, parent) match {
        case Some((_, _: AmfScalar, Some(value: String))) =>
          if (!(maxLength.toInt > value.length)) {
            reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
          }

        case _ => // ignore
      }
    }
  }
}
