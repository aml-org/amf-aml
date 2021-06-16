package amf.validation.internal.shacl.custom.validators

import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfObject}
import amf.core.internal.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.validation.internal.shacl.custom.{PropertyConstraintValidator, ReportBuilder}
import amf.validation.internal.shacl.custom.PropertyConstraintValidator.extractPropertyValue

case object MaxCountConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.maxCount.isDefined

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: AmfObject,
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
