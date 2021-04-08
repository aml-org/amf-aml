package amf.plugins.features.validation.shacl.custom.validators

import amf.core.model.domain.{AmfArray, AmfElement, DomainElement}
import amf.core.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}
import amf.plugins.features.validation.shacl.custom.PropertyConstraintValidator.extractPropertyValue

case object MaxCountConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.maxCount.isDefined

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: DomainElement,
                        reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.maxCount.foreach { maxCount =>
      extractPropertyValue(propertyConstraint, parent) match {

        case Some((_, arr: AmfArray, _)) =>
          if (!(arr.values.length <= maxCount.toInt)) {
            reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
          }

        case Some((_, _: AmfElement, _)) =>
          if (!(1 <= maxCount.toInt)) {
            reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
          }

        case _ =>
        // ignore
      }
    }
  }
}
