package amf.plugins.features.validation.shacl.custom.validators

import amf.core.model.domain.{AmfObject, AmfScalar}
import amf.core.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.PropertyConstraintValidator.extractPropertyValue
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}

case object MinInclusiveConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.minInclusive.isDefined

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: AmfObject,
                        reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.minInclusive.foreach { minInclusive =>
      extractPropertyValue(propertyConstraint, parent) match {
        case Some((_, _: AmfScalar, Some(value: Long))) =>
          if (propertyConstraint.minInclusive.get.contains(".")) {
            if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(propertyConstraint.minInclusive.get.toLong <= value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case Some((_, _: AmfScalar, Some(value: Integer))) =>
          if (propertyConstraint.minInclusive.get.contains(".")) {
            if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(propertyConstraint.minInclusive.get.toInt <= value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case Some((_, _: AmfScalar, Some(value: Float))) =>
          if (propertyConstraint.minInclusive.get.contains(".")) {
            if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(propertyConstraint.minInclusive.get.toFloat <= value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case Some((_, _: AmfScalar, Some(value: Double))) =>
          if (propertyConstraint.minInclusive.get.contains(".")) {
            if (!(propertyConstraint.minInclusive.get.toDouble <= value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(propertyConstraint.minInclusive.get.toFloat <= value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case _ => // ignore
      }
    }
  }
}
