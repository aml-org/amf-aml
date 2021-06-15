package amf.validation.internal.shacl.custom.validators

import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfObject, AmfScalar, DomainElement}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.validation.core.{NodeConstraint, ValidationSpecification}
import amf.validation.internal.shacl.custom.PropertyConstraintValidator.extractPredicateValue
import amf.validation.internal.shacl.custom.{ConstraintValidator, ReportBuilder}

case object TargetObjectsOfConstraint extends ConstraintValidator {
  override def canValidate(spec: ValidationSpecification): Boolean = spec.targetObject.nonEmpty

  // this is always (?s sh:nodeKind sh:IRI), we still put the checking logic in place
  override def validate(spec: ValidationSpecification, element: AmfObject, reportBuilder: ReportBuilder): Unit = {
    spec.targetObject.foreach { property =>
      findFieldTarget(element, property) match {
        case Some((_: Annotations, objectsOf: AmfArray)) =>
          objectsOf.foreach {
            case obj: DomainElement =>
              spec.nodeConstraints.foreach { nodeConstraint =>
                validateNodeConstraint(spec, nodeConstraint, obj, reportBuilder)
              }
            case _ => // ignore
          }
        case _ => // ignore
      }
    }
  }

  private def validateNodeConstraint(validationSpecification: ValidationSpecification,
                                     nodeConstraint: NodeConstraint,
                                     element: AmfObject,
                                     reportBuilder: ReportBuilder): Unit = {
    val nodeKindIri = (Namespace.Shacl + "nodeKind").iri()
    val shaclIri    = (Namespace.Shacl + "IRI").iri()

    nodeConstraint.constraint match {
      case s if s == nodeKindIri =>
        nodeConstraint.value match {
          case v if v == shaclIri =>
            validationSpecification.targetObject.foreach { targetObject =>
              extractPredicateValue(targetObject, element) match {
                case Some((_, _: AmfScalar, Some(value: String))) =>
                  if (!value.contains("://")) {
                    reportBuilder.reportFailure(validationSpecification, element.id)
                  }
                case _ => // ignore
              }
            }
          case other =>
            throw new Exception(s"Not supported node constraint range $other")
        }
      case other =>
        throw new Exception(s"Not supported node constraint $other")
    }
  }

  private def findFieldTarget(element: AmfObject, property: String): Option[(Annotations, Seq[AmfElement])] = {
    findFieldWithIri(element, property).flatMap(f => element.fields.getValueAsOption(f)).map { value =>
      value.value match {
        case elems: AmfArray   => (value.annotations, elems.values)
        case scalar: AmfScalar => (value.annotations, Seq(scalar))
        case obj: AmfObject    => (value.annotations, Seq(obj))
        case _                 => (value.annotations, Nil)
      }
    }
  }

  private def findFieldWithIri(element: AmfObject, iri: String) = {
    element.meta.fields.find(_.value.iri() == iri)
  }
}
