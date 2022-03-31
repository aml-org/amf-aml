package amf.validation.internal.shacl.custom

import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfScalar, DomainElement}
import amf.core.internal.annotations.SourceAST
import amf.core.internal.validation.core.PropertyConstraint
import org.yaml.model.YScalar

case class ExtractedPropertyValue(value: AmfElement, nativeScalar: Option[Any])

trait ElementExtractor {
  def extractPlainPropertyValue(propertyConstraint: PropertyConstraint,
                                element: DomainElement): Seq[ExtractedPropertyValue]

  def extractPlainPredicateValue(predicate: String, element: DomainElement): Seq[ExtractedPropertyValue]

  def extractPropertyValue(propertyConstraint: PropertyConstraint,
                           element: DomainElement): Option[ExtractedPropertyValue]
}

object DefaultElementExtractor extends ElementExtractor {
  def extractPlainPropertyValue(propertyConstraint: PropertyConstraint,
                                element: DomainElement): Seq[ExtractedPropertyValue] =
    extractPlainPredicateValue(propertyConstraint.ramlPropertyId, element)

  def extractPlainPredicateValue(predicate: String, element: DomainElement): Seq[ExtractedPropertyValue] =
    extractElement(predicate, element).map(toNativeScalar).getOrElse(Nil)

  def extractPropertyValue(propertyConstraint: PropertyConstraint,
                           element: DomainElement): Option[ExtractedPropertyValue] = {
    extractElement(propertyConstraint.ramlPropertyId, element).map {
      case s: AmfScalar =>
        ExtractedPropertyValue(s, Some(amfScalarToScala(s)))
      case a: AmfArray =>
        ExtractedPropertyValue(a, None)
      case other =>
        ExtractedPropertyValue(other, None)
    }
  }

  private def amfScalarToScala(scalar: AmfScalar): Any = {
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

  private def toNativeScalar(element: AmfElement): Seq[ExtractedPropertyValue] = {
    element match {
      case s: AmfScalar => Seq(ExtractedPropertyValue(s, Some(amfScalarToScala(s))))
      case r: AmfArray =>
        r.values.flatMap(toNativeScalar)
      case _ => Seq(ExtractedPropertyValue(element, None))
    }
  }

  private def extractElement(fieldUri: String, element: DomainElement): Option[AmfElement] =
    element.fields.getValueAsOption(fieldUri).map(_.value)
}

class ScalarElementExtractor(scalar: AmfScalar) extends ElementExtractor {
  override def extractPlainPropertyValue(propertyConstraint: PropertyConstraint,
                                         element: DomainElement): Seq[ExtractedPropertyValue] = Seq(value())

  override def extractPlainPredicateValue(predicate: String, element: DomainElement): Seq[ExtractedPropertyValue] =
    Seq(value())

  override def extractPropertyValue(propertyConstraint: PropertyConstraint,
                                    element: DomainElement): Option[ExtractedPropertyValue] = {
    Some(value())
  }

  private def value() = ExtractedPropertyValue(scalar, Option(scalar.value))
}
