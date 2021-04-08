package amf.plugins.features.validation.shacl.custom.validators

import amf.core.model.domain.{AmfArray, AmfElement, AmfObject, AmfScalar, DomainElement}
import amf.core.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}
import amf.plugins.features.validation.shacl.custom.PropertyConstraintValidator.extractPropertyValue

case object MinCountConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.minCount.isDefined

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: DomainElement,
                        reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.minCount.foreach { minCount =>
      extractPropertyValue(propertyConstraint, parent) match {
        case Some((_, arr: AmfArray, _)) =>
          if (!(arr.values.length >= minCount.toInt)) {
            reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
          }

        // cases scalar and object are equals, but we need to match by specific class because in api designer
        // qax environment the match does not work with the trait amfElement class
        case Some((_, _: AmfScalar, _)) =>
          if (!(1 >= minCount.toInt)) {
            reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
          }

        case Some((_, _: AmfObject, _)) =>
          if (!(1 >= minCount.toInt)) {
            reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
          }

        case _ =>
          if (minCount != "0")
            reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
      }
    }
  }
}
