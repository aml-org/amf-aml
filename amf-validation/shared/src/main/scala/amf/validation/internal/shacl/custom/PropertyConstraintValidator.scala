package amf.validation.internal.shacl.custom

import amf.core.client.scala.model.domain.{AmfElement, AmfObject, AmfScalar}
import amf.core.internal.annotations.{SourceAST, SourceYPart}
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.validation.core.{PropertyConstraint, ValidationSpecification}
import org.yaml.model.YScalar

object PropertyConstraintValidator {
  def extractPropertyValue(
      propertyConstraint: PropertyConstraint,
      element: AmfObject
  ): Option[(Annotations, AmfElement, Option[Any])] = {
    extractPredicateValue(propertyConstraint.ramlPropertyId, element)
  }

  def extractPredicateValue(predicate: String, element: AmfObject): Option[(Annotations, AmfElement, Option[Any])] = {
    element.meta.fields.find { f =>
      f.value.iri() == predicate
    } match {
      case Some(f) =>
        Option(element.fields.getValue(f)) match {
          case Some(value) if value.value.isInstanceOf[AmfScalar] =>
            Some((value.annotations, value.value, Some(amfScalarToScala(value.value.asInstanceOf[AmfScalar]))))
          case Some(value) =>
            Some((value.annotations, value.value, None))
          case _ =>
            None
        }
      case _ => None
    }
  }

  def amfScalarToScala(scalar: AmfScalar): Any = {
    scalar.annotations.find(classOf[SourceAST[_]]) match {
      case Some(ast: SourceYPart) =>
        ast.ast match {
          case yscalar: YScalar => yscalar.value
          case _                => scalar.value
        }
      case Some(_) => scalar.value
      case None =>
        scalar.value
    }
  }
}

trait ConstraintValidator {
  def canValidate(spec: ValidationSpecification): Boolean
  def validate(spec: ValidationSpecification, element: AmfObject, reportBuilder: ReportBuilder): Unit
}

trait PropertyConstraintValidator {

  def canValidate(spec: PropertyConstraint): Boolean
  def validate(
      spec: ValidationSpecification,
      propertyConstraint: PropertyConstraint,
      parent: AmfObject,
      reportBuilder: ReportBuilder
  )
}
