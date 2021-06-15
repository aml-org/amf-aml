package amf.validation.internal.shacl.custom.validators

import amf.core.client.scala.model.domain.{AmfObject, AmfScalar}
import amf.core.internal.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.validation.internal.shacl.custom.PropertyConstraintValidator.extractPropertyValue
import amf.validation.internal.shacl.custom.{PropertyConstraintValidator, ReportBuilder}

case object MaxInclusiveConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.maxInclusive.isDefined

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: AmfObject,
                        reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.maxInclusive.foreach { maxInclusive =>
      extractPropertyValue(propertyConstraint, parent) match {
        case Some((_, _: AmfScalar, Some(value: Long))) =>
          if (propertyConstraint.maxInclusive.get.contains(".")) {
            if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(propertyConstraint.maxInclusive.get.toLong >= value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case Some((_, _: AmfScalar, Some(value: Integer))) =>
          if (propertyConstraint.maxInclusive.get.contains(".")) {
            if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(propertyConstraint.maxInclusive.get.toInt >= value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case Some((_, _: AmfScalar, Some(value: Float))) =>
          if (propertyConstraint.maxInclusive.get.contains(".")) {
            if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(propertyConstraint.maxInclusive.get.toFloat >= value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case Some((_, _: AmfScalar, Some(value: Double))) =>
          if (propertyConstraint.maxInclusive.get.contains(".")) {
            if (!(propertyConstraint.maxInclusive.get.toDouble >= value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(propertyConstraint.maxInclusive.get.toFloat >= value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case _ => // ignore
      }
    }
  }
}
