package amf.plugins.features.validation.shacl.custom.validators

import amf.core.client.scala.model.domain.AmfObject
import amf.core.internal.validation.core.ValidationSpecification
import amf.plugins.features.validation.shacl.custom.{ConstraintValidator, PropertyConstraintValidator, ReportBuilder}

object UnsupportedConstraint {
  protected[shacl] val closed = UnsupportedConstraint(_.closed.isDefined, "Closed constraint not supported yet: ")
  protected[shacl] val custom =
    UnsupportedConstraint(_.closed.isDefined, "Arbitray SHACL validations not supported in custom SHACL validator: ")
}

case class UnsupportedConstraint(private val applies: ValidationSpecification => Boolean, private val message: String)
    extends ConstraintValidator {
  override def canValidate(spec: ValidationSpecification): Boolean = false

  override def validate(spec: ValidationSpecification, element: AmfObject, reportBuilder: ReportBuilder): Unit = {
    if (applies(spec)) throw new Exception(s"$message ${spec.id}")
  }
}
