package amf.plugins.features.validation.shacl.custom.validators

import amf.core.model.domain.DomainElement
import amf.core.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}

object UnsupportedPropertyConstraint {
  private[shacl] val `class`        = UnsupportedPropertyConstraint(_.`class`.nonEmpty, "class")
  private[shacl] val customProperty = UnsupportedPropertyConstraint(_.custom.isDefined, "custom")
  private[shacl] val customRdf      = UnsupportedPropertyConstraint(_.customRdf.isDefined, "customRdf")
  private[shacl] val multipleOf     = UnsupportedPropertyConstraint(_.customRdf.isDefined, "multipleOf")
  private[shacl] val patternedProperty =
    UnsupportedPropertyConstraint(_.patternedProperty.isDefined, "patternedProperty")
}

case class UnsupportedPropertyConstraint(private val applies: PropertyConstraint => Boolean,
                                         private val constraintName: String)
    extends PropertyConstraintValidator {
  override def canValidate(spec: PropertyConstraint): Boolean = false

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: DomainElement,
                        reportBuilder: ReportBuilder): Unit = {
    if (applies(propertyConstraint))
      throw new Exception(s"$constraintName property constraint not supported yet ${spec.id}")
  }
}
