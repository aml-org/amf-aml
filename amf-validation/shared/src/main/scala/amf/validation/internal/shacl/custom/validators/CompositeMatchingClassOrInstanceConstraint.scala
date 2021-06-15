package amf.validation.internal.shacl.custom.validators

import amf.core.client.scala.model.domain.AmfObject
import amf.core.internal.validation.core.ValidationSpecification
import amf.validation.internal.shacl.custom.{ConstraintValidator, ReportBuilder}

case class CompositeMatchingClassOrInstanceConstraint(constraints: Set[ConstraintValidator])
    extends ConstraintValidator {
  override def canValidate(spec: ValidationSpecification): Boolean = {
    val hasTarget = spec.targetClass.nonEmpty || spec.targetInstance.nonEmpty
    hasTarget && constraints.exists(p => p.canValidate(spec))
  }

  override def validate(spec: ValidationSpecification, element: AmfObject, reportBuilder: ReportBuilder): Unit = {
    if (matchesNode(spec, element)) constraints.foreach(c => c.validate(spec, element, reportBuilder))
  }

  private def matchesNode(specification: ValidationSpecification, element: AmfObject) = {
    matchesClass(specification, element) || matchesInstance(specification, element)
  }

  private def metaClassIris(element: AmfObject) = element.meta.`type`.map(_.iri())

  private def matchesClass(specification: ValidationSpecification, element: AmfObject): Boolean = {
    val classes = metaClassIris(element)
    specification.targetClass.exists(classes.contains)
  }

  private def matchesInstance(specification: ValidationSpecification, element: AmfObject): Boolean =
    specification.targetInstance.contains(element.id)
}
