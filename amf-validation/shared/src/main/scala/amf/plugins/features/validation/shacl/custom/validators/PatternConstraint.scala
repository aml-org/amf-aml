package amf.plugins.features.validation.shacl.custom.validators

import amf.core.model.domain.{AmfArray, AmfScalar, DomainElement}
import amf.core.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.PropertyConstraintValidator.extractPropertyValue
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}

case object PatternConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.pattern.isDefined

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: DomainElement,
                        reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.pattern.foreach { pattern =>
      extractPropertyValue(propertyConstraint, parent) match {
        case Some((_, _: AmfScalar, Some(value))) =>
          if (valueDoesntComplyWithPattern(propertyConstraint, value))
            reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
        case Some((_, arr: AmfArray, _)) =>
          arr.values.foreach {
            case value: AmfScalar =>
              if (Option(value).isDefined && propertyConstraint.pattern.get.r.findFirstIn(value.toString).isEmpty)
                reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            case _ => // ignore
          }
        case _ => // ignore
      }
    }
  }

  private def valueDoesntComplyWithPattern(propertyConstraint: PropertyConstraint, value: Any) = {
    Option(value).isDefined && propertyConstraint.pattern.get.r.findFirstIn(value.toString).isEmpty
  }
}
