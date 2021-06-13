package amf.plugins.features.validation.shacl.custom.validators

import amf.core.client.scala.model.domain.AmfObject
import amf.core.internal.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.{ConstraintValidator, PropertyConstraintValidator, ReportBuilder}

case class CompositePropertyConstraint(constraints: Seq[PropertyConstraintValidator]) extends ConstraintValidator {
  override def canValidate(spec: ValidationSpecification): Boolean =
    spec.propertyConstraints.nonEmpty && spec.propertyConstraints.forall(p =>
      !referencesNestedField(p) && constraints.exists(_.canValidate(p)))

  private def referencesNestedField(constraint: PropertyConstraint) = constraint.path.isDefined

  override def validate(spec: ValidationSpecification, element: AmfObject, reportBuilder: ReportBuilder): Unit = {
    spec.propertyConstraints.foreach { propertyConstraint =>
      constraints.foreach(_.validate(spec, propertyConstraint, element, reportBuilder))
    }
  }
}
