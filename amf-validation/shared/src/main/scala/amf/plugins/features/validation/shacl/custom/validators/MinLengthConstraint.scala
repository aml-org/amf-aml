package amf.plugins.features.validation.shacl.custom.validators

import amf.core.client.scala.model.domain.{AmfObject, AmfScalar}
import amf.core.internal.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.PropertyConstraintValidator.extractPropertyValue
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}

case object MinLengthConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.minLength.isDefined

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: AmfObject,
                        reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.minLength.foreach { minLength =>
      extractPropertyValue(propertyConstraint, parent) match {
        case Some((_, _: AmfScalar, Some(value: String))) =>
          if (!(minLength.toInt <= value.length)) {
            reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
          }

        case Some((_, _: AmfScalar, Some(x)))
            if Option(x).isEmpty => // this happens in cases where the value of a key in YAML is the empty string
          if (!(minLength.toInt <= 0)) {
            reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
          }

        case _ => // ignore
      }
    }
  }
}
