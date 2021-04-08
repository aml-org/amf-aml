package amf.plugins.features.validation.shacl.custom

import amf.core.annotations.SourceAST
import amf.core.model.domain.{AmfElement, AmfScalar, DomainElement}
import amf.core.parser.Annotations
import amf.core.validation.core.{NodeConstraint, PropertyConstraint, ValidationSpecification}
import org.yaml.model.YScalar

object PropertyConstraintValidator {
  def extractPropertyValue(propertyConstraint: PropertyConstraint,
                           element: DomainElement): Option[(Annotations, AmfElement, Option[Any])] = {
    extractPredicateValue(propertyConstraint.ramlPropertyId, element)
  }

  def extractPredicateValue(predicate: String,
                            element: DomainElement): Option[(Annotations, AmfElement, Option[Any])] = {
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
    scalar.annotations.find(classOf[SourceAST]) match {
      case Some(ast: SourceAST) =>
        ast.ast match {
          case yscalar: YScalar => yscalar.value
          case _                => scalar.value
        }

      case None =>
        scalar.value
    }
  }
}

trait ConstraintValidator {
  def canValidate(spec: ValidationSpecification): Boolean
  def validate(spec: ValidationSpecification, parent: DomainElement, reportBuilder: ReportBuilder)
}

trait PropertyConstraintValidator {

  def canValidate(spec: PropertyConstraint): Boolean
  def validate(spec: ValidationSpecification,
               propertyConstraint: PropertyConstraint,
               parent: DomainElement,
               reportBuilder: ReportBuilder)
}
