package amf.plugins.features.validation.shacl.custom.validators

import amf.core.client.scala.model.domain.{AmfArray, AmfObject}
import amf.core.internal.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.PropertyConstraintValidator.extractPropertyValue
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}

case object PropertyNodeConstraint extends PropertyConstraintValidator {

  override def canValidate(spec: PropertyConstraint): Boolean = spec.node.exists(_.endsWith("NonEmptyList"))

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: AmfObject,
                        reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.node.foreach { node =>
      if (node.endsWith("NonEmptyList")) {
        extractPropertyValue(propertyConstraint, parent) match {
          case Some((_, arr: AmfArray, _)) =>
            if (arr.values.isEmpty) {
              reportBuilder.reportFailure(spec, propertyConstraint, parent.id)
            }
          case _ => // ignore
        }
      } else {
        throw new Exception(s"Unsupported property node value $node")
      }
    }
  }
}
