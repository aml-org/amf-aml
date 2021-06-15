package amf.plugins.features.validation.shacl.custom.validators

import amf.core.client.scala.model.domain.{AmfObject, AmfScalar}
import amf.core.internal.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.PropertyConstraintValidator.extractPropertyValue
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}

case object MaxExclusiveConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.maxExclusive.isDefined

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: AmfObject,
                        reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.maxExclusive.foreach { maxExclusive =>
      extractPropertyValue(propertyConstraint, parent) match {
        case Some((_, _: AmfScalar, Some(value: Long))) =>
          if (maxExclusive.contains(".")) {
            if (!(maxExclusive.toDouble > value.toDouble)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(maxExclusive.toLong > value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case Some((_, _: AmfScalar, Some(value: Integer))) =>
          if (maxExclusive.contains(".")) {
            if (!(maxExclusive.toDouble > value.toDouble)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(maxExclusive.toInt > value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case Some((_, _: AmfScalar, Some(value: Float))) =>
          if (maxExclusive.contains(".")) {
            if (!(maxExclusive.toDouble > value.toDouble)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(maxExclusive.toFloat > value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case Some((_, _: AmfScalar, Some(value: Double))) =>
          if (maxExclusive.contains(".")) {
            if (!(maxExclusive.toDouble > value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          } else {
            if (!(maxExclusive.toFloat > value)) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          }

        case _ => // ignore
      }
    }
  }
}
