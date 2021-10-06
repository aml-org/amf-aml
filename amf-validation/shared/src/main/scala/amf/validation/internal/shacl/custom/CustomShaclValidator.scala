package amf.validation.internal.shacl.custom

import amf.core.client.common.validation.MessageStyle
import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain._
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.annotations.SourceAST
import amf.core.internal.metamodel.Field
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.utils._
import amf.core.internal.validation.core._
import amf.validation.internal.shacl.custom.CustomShaclValidator.{
  CustomShaclFunction,
  CustomShaclFunctions,
  ValidationInfo
}
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.YScalar

import java.net.URISyntaxException
import scala.concurrent.{ExecutionContext, Future}

object CustomShaclValidator {

  case class ValidationInfo(field: Field, message: Option[String] = None, annotations: Option[Annotations] = None)
  trait CustomShaclFunction {
    val name: String
    // When no validation info is provided, the validation is thrown in domain element level
    def run(element: AmfObject, validate: Option[ValidationInfo] => Unit): Unit
  }
  type CustomShaclFunctions = Map[String, CustomShaclFunction]
}

class CustomShaclValidator(customFunctions: CustomShaclFunctions, messageStyle: MessageStyle) {

  private val reportBuilder: ReportBuilder = new ReportBuilder(messageStyle)

  def validate(unit: BaseUnit, validations: Seq[ValidationSpecification])(
      implicit executionContext: ExecutionContext): Future[ValidationReport] = {
    unit.iterator().foreach {
      case e: DomainElement => validateIdentityTransformation(validations, e)
      case _                =>
    }
    Future.successful(reportBuilder.build())
  }

  private def validateIdentityTransformation(validations: Seq[ValidationSpecification], element: DomainElement): Unit = {
    val classes        = element.meta.`type`.map(_.iri())
    val isExternalLink = element.isExternalLink.value()
    validations.foreach { specification =>
      if (!isExternalLink && (matchingClass(specification, classes) || matchingInstance(specification, element))) {
        validate(specification, element)
      }
      validateObjectsOf(specification, element)
    }
  }

  // this is always (?s sh:nodeKind sh:IRI), we still put the checking logic in place
  private def validateObjectsOf(validationSpecification: ValidationSpecification, element: DomainElement): Unit = {
    validationSpecification.targetObject.foreach { property =>
      findFieldTarget(element, property) match {
        case Some((_: Annotations, objectsOf: AmfArray)) =>
          objectsOf.foreach {
            case obj: DomainElement =>
              validationSpecification.nodeConstraints.foreach { nodeConstraint =>
                validateNodeConstraint(validationSpecification, nodeConstraint, obj)
              }
            case _ => // ignore
          }
        case _ => // ignore
      }
    }
  }

  // TODO: could this made be faster by using Sets? -> We would have to propagate Sets to several places
  private def matchingClass(specification: ValidationSpecification, classes: Seq[String]): Boolean = {
    specification.targetClass.exists(classes.contains)
  }

  private def matchingInstance(specification: ValidationSpecification, element: DomainElement): Boolean =
    specification.targetInstance.contains(element.id)

  private def findFieldTarget(element: DomainElement, property: String): Option[(Annotations, Seq[AmfElement])] = {
    element.meta.fields.find(_.value.iri() == property) match {
      case Some(field) =>
        Option(element.fields.getValue(field)) match {
          case Some(value) =>
            value.value match {
              case elems: AmfArray   => Some((value.annotations, elems.values))
              case scalar: AmfScalar => Some((value.annotations, Seq(scalar)))
              case obj: AmfObject    => Some((value.annotations, Seq(obj)))
              case _                 => Some((value.annotations, Nil))
            }
          case _ => None
        }
      case _ => None
    }
  }

  private case class ExtractedPropertyValue(value: AmfElement, nativeScalar: Option[Any])

  private def toNativeScalar(element: AmfElement): Seq[ExtractedPropertyValue] = {
    element match {
      case s: AmfScalar => Seq(ExtractedPropertyValue(s, Some(amfScalarToScala(s))))
      case r: AmfArray =>
        r.values.flatMap(toNativeScalar)
      case _ => Seq(ExtractedPropertyValue(element, None))
    }
  }

  private def extractPlainPropertyValue(propertyConstraint: PropertyConstraint,
                                        element: DomainElement): Seq[ExtractedPropertyValue] =
    extractPlainPredicateValue(propertyConstraint.ramlPropertyId, element)

  private def extractPlainPredicateValue(predicate: String, element: DomainElement): Seq[ExtractedPropertyValue] =
    extractElement(predicate, element).map(toNativeScalar).getOrElse(Nil)

  private def extractPropertyValue(propertyConstraint: PropertyConstraint,
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

  private def extractElement(predicate: String, element: DomainElement) = {
    element.meta.fields
      .find(f => f.value.iri() == predicate)
      .flatMap(f => Option(element.fields.getValue(f)))
      .map(_.value)
  }

  private def validate(validationSpecification: ValidationSpecification, element: DomainElement): Unit = {
    validationSpecification.closed match {
      case Some(_) => validateClosed(validationSpecification)
      case _       => // ignore
    }
    validationSpecification.custom match {
      case Some(_) => validateCustom(validationSpecification)
      case _       => // ignore
    }
    validationSpecification.functionConstraint match {
      case Some(_) => validateFunctionConstraint(validationSpecification, element)
      case _       => // ignore
    }

    validationSpecification.replacesFunctionConstraint match {
      // some JS functions have been replaced by custom validations.
      // The custom SHACL validator cannot execute them, it still relies on the custom Scala function
      case Some(functionConstraintName) =>
        validateFunctionConstraint(
            validationSpecification.copy(
                functionConstraint = Some(
                    FunctionConstraint(
                        message = Some(validationSpecification.message),
                        internalFunction = Some(functionConstraintName)
                    ))),
            element
        )
      // Normal constraints here
      case _ =>
        validationSpecification.propertyConstraints.foreach { propertyConstraint =>
          validatePropertyConstraint(validationSpecification, propertyConstraint, element)
        }
    }

  }

  private def validateClosed(validationSpecification: ValidationSpecification): Unit = {
    throw new Exception(s"Closed constraint not supported yet: ${validationSpecification.id}")
  }

  private def validateCustom(validationSpecification: ValidationSpecification): Unit = {
    throw new Exception(
        s"Arbitrary SHACL validations not supported in custom SHACL validator: ${validationSpecification.id}")
  }

  private def validateFunctionConstraint(validationSpecification: ValidationSpecification,
                                         element: DomainElement): Unit = {
    val functionConstraint = validationSpecification.functionConstraint.get
    functionConstraint.internalFunction.foreach(name => {
      val validationFunction = getFunctionForName(name)
      // depending if propertyInfo is provided, violation is thrown at a given property, or by default on element
      val onValidation = (validationInfo: Option[ValidationInfo]) =>
        validationInfo match {
          case Some(ValidationInfo(field, customMessage, _)) => // why annotations are never used?
            reportFailure(validationSpecification, element.id, field.toString, customMessage)
          case _ => reportFailure(validationSpecification, element.id, "")
      }
      validationFunction.run(element, onValidation)
    })
  }

  private def getFunctionForName(name: String): CustomShaclFunction = customFunctions.get(name) match {
    case Some(validationFunction) => validationFunction
    case None =>
      throw new Exception(s"Custom function validations not supported in custom SHACL validator: $name")
  }

  private def validateNodeConstraint(validationSpecification: ValidationSpecification,
                                     nodeConstraint: NodeConstraint,
                                     element: DomainElement): Unit = {
    val nodeKindIri = (Namespace.Shacl + "nodeKind").iri()
    val shaclIri    = (Namespace.Shacl + "IRI").iri()

    nodeConstraint.constraint match {
      case s if s == nodeKindIri =>
        nodeConstraint.value match {
          case v if v == shaclIri =>
            validationSpecification.targetObject.foreach { targetObject =>
              extractPlainPredicateValue(targetObject, element).foreach {
                case ExtractedPropertyValue(_: AmfScalar, Some(value: String)) if !value.contains("://") =>
                  reportFailure(validationSpecification, element.id, "")
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

  private def validatePropertyConstraint(validationSpecification: ValidationSpecification,
                                         propertyConstraint: PropertyConstraint,
                                         element: DomainElement): Unit = {
    propertyConstraint.node match {
      case Some(_) => validatePropertyNode(validationSpecification, propertyConstraint, element)
      case _       =>
    }
    propertyConstraint.maxCount match {
      case Some(_) => validateMaxCount(validationSpecification, propertyConstraint, element)
      case _       =>
    }
    propertyConstraint.minCount match {
      case Some(_) => validateMinCount(validationSpecification, propertyConstraint, element)
      case _       =>
    }
    propertyConstraint.maxLength match {
      case Some(_) => validateMaxLength(validationSpecification, propertyConstraint, element)
      case _       =>
    }
    propertyConstraint.minLength match {
      case Some(_) => validateMinLength(validationSpecification, propertyConstraint, element)
      case _       =>
    }
    propertyConstraint.in.toList match {
      case Nil => // ignore
      case l   => validateIn(validationSpecification, propertyConstraint, element)
    }
    propertyConstraint.maxExclusive match {
      case Some(_) => validateMaxExclusive(validationSpecification, propertyConstraint, element)
      case _       => // ignore
    }
    propertyConstraint.minExclusive match {
      case Some(_) => validateMinExclusive(validationSpecification, propertyConstraint, element)
      case _       => // ignore
    }
    propertyConstraint.maxInclusive match {
      case Some(_) => validateMaxInclusive(validationSpecification, propertyConstraint, element)
      case _       => // ignore
    }
    propertyConstraint.minInclusive match {
      case Some(_) => validateMinInclusive(validationSpecification, propertyConstraint, element)
      case _       => // ignore
    }
    propertyConstraint.pattern match {
      case Some(_) => validatePattern(validationSpecification, propertyConstraint, element)
      case _       => // ignore
    }
    propertyConstraint.datatype match {
      case Some(_) => validateDataType(validationSpecification, propertyConstraint, element)
      case _       => // ignore
    }
    //
    // These are only used for payload validations
    //
    propertyConstraint.`class` match {
      case Nil => // ignore
      case _   => validateClass(validationSpecification, propertyConstraint, element)
    }
    if (propertyConstraint.custom.isDefined) {
      throw new Exception(s"custom property constraint not supported yet ${validationSpecification.id}")
    }
    if (propertyConstraint.multipleOf.isDefined) {
      throw new Exception(s"multipleOf property constraint not supported yet ${validationSpecification.id}")
    }
    if (propertyConstraint.patternedProperty.isDefined) {
      throw new Exception(s"patternedProperty property constraint not supported yet ${validationSpecification.id}")
    }
  }

  private def validateClass(validationSpecification: ValidationSpecification,
                            propertyConstraint: PropertyConstraint,
                            element: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, element).foreach {
      case ExtractedPropertyValue(obj: AmfObject, _) =>
        val current = obj.meta.`type`.map(_.iri())
        if (!propertyConstraint.`class`.exists(t => current.contains(t)))
          reportBuilder.reportFailure(validationSpecification, propertyConstraint, element.id)
      case _ => // ignore
    }
  }

  private def validatePropertyNode(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement): Unit = {
    if (propertyConstraint.node.get.endsWith("NonEmptyList")) {
      extractPropertyValue(propertyConstraint, parentElement) match {
        case Some(ExtractedPropertyValue(arr: AmfArray, _)) =>
          if (arr.values.isEmpty) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }

        case _ => // ignore
      }
    } else {
      throw new Exception(s"Unsupported property node value ${propertyConstraint.node.get}")
    }
  }

  private def validateMinCount(validationSpecification: ValidationSpecification,
                               propertyConstraint: PropertyConstraint,
                               parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some(ExtractedPropertyValue(arr: AmfArray, _)) =>
        if (!(arr.values.length >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      // cases scalar and object are equals, but we need to match by specific class because in api designer
      // qax environment the match does not work with the trait amfElement class
      case Some(ExtractedPropertyValue(_: AmfScalar, _)) =>
        if (!(1 >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case Some(ExtractedPropertyValue(_: AmfObject, _)) =>
        if (!(1 >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case _ =>
        if (!propertyConstraint.minCount.contains("0"))
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
    }
  }

  private def validateMaxCount(validationSpecification: ValidationSpecification,
                               propertyConstraint: PropertyConstraint,
                               parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {

      case Some(ExtractedPropertyValue(arr: AmfArray, _)) =>
        if (!(arr.values.length <= propertyConstraint.maxCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case Some(ExtractedPropertyValue(_: AmfElement, _)) =>
        if (!(1 <= propertyConstraint.maxCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case _ =>
      // ignore
    }
  }

  private def validateMinLength(validationSpecification: ValidationSpecification,
                                propertyConstraint: PropertyConstraint,
                                parentElement: DomainElement): Unit = {
    extractPlainPropertyValue(propertyConstraint, parentElement) match {
      case Seq(ExtractedPropertyValue(_: AmfScalar, Some(value: String))) =>
        if (!(propertyConstraint.minLength.get.toInt <= value.length)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case Seq(ExtractedPropertyValue(_: AmfScalar, Some(x)))
          if Option(x).isEmpty => // this happens in cases where the value of a key in YAML is the empty string
        if (!(propertyConstraint.minLength.get.toInt <= 0)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case _ => // ignore
    }
  }

  private def validateMaxLength(validationSpecification: ValidationSpecification,
                                propertyConstraint: PropertyConstraint,
                                parentElement: DomainElement): Unit = {
    extractPlainPropertyValue(propertyConstraint, parentElement) match {
      case Seq(ExtractedPropertyValue(_: AmfScalar, Some(value: String))) =>
        if (!(propertyConstraint.maxLength.get.toInt > value.length)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case _ => // ignore
    }
  }

  private def validateIn(validationSpecification: ValidationSpecification,
                         propertyConstraint: PropertyConstraint,
                         parentElement: DomainElement): Unit = {
    extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, Some(value: String)) =>
        if (!propertyConstraint.in.contains(value)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case _ => // ignore
    }
  }

  private def validateMaxInclusive(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement): Unit = {
    extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, Some(value: Long)) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toLong >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Integer)) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toInt >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Float)) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toFloat >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Double)) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toFloat >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case _ => // ignore
    }
  }

  private def validateMaxExclusive(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement): Unit = {
    extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, Some(value: Long)) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toLong > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Integer)) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toInt > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Float)) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toFloat > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Double)) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toFloat > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case _ => // ignore
    }
  }

  private def validateMinInclusive(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement): Unit = {
    extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, Some(value: Long)) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toLong <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Integer)) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toInt <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Float)) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toFloat <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Double)) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toFloat <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case _ => // ignore
    }
  }

  private def validateMinExclusive(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement): Unit = {
    extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, Some(value: Long)) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toLong < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Integer)) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toInt < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Float)) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toFloat < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Double)) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toFloat < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case _ => // ignore
    }
  }

  private def validatePattern(validationSpecification: ValidationSpecification,
                              propertyConstraint: PropertyConstraint,
                              parentElement: DomainElement): Unit = {
    extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(scalar: AmfScalar, _) =>
        if (valueDoesntComplyWithPattern(propertyConstraint, scalar))
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
      case _ => // ignore
    }
  }

  private def valueDoesntComplyWithPattern(propertyConstraint: PropertyConstraint, value: AmfScalar) = {
    Option(value).isDefined && propertyConstraint.pattern.get.r.findFirstIn(value.toString).isEmpty
  }

  private def validateDataType(validationSpecification: ValidationSpecification,
                               propertyConstraint: PropertyConstraint,
                               parentElement: DomainElement): Unit = {
    val xsdString       = DataType.String
    val xsdBoolean      = DataType.Boolean
    val xsdInteger      = DataType.Integer
    val xsdDouble       = DataType.Double
    val xsdDate         = DataType.Date
    val xsdDateTime     = DataType.DateTime
    val xsdDateTimeOnly = DataType.DateTimeOnly
    val xsdTime         = DataType.Time
    val xsdAnyURI       = DataType.AnyUri

    extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, maybeScalarValue) =>
        propertyConstraint.datatype match {
          case Some(s) if s == xsdString => // ignore

          case Some(s) if s == xsdBoolean =>
            maybeScalarValue match {
              case Some(_: Boolean) => // ignore
              case _ =>
                reportFailure(validationSpecification, propertyConstraint, parentElement.id)
            }
          case Some(s) if s == xsdInteger =>
            maybeScalarValue match {
              case Some(_: Integer) => // ignore
              case Some(_: Long)    => // ignore
              case _ =>
                reportFailure(validationSpecification, propertyConstraint, parentElement.id)
            }

          case Some(s) if s == xsdDouble =>
            maybeScalarValue match {
              case Some(_: Integer) => // ignore
              case Some(_: Long)    => // ignore
              case Some(_: Double)  => // ignore
              case Some(_: Float)   => // ignore
              case _ =>
                reportFailure(validationSpecification, propertyConstraint, parentElement.id)
            }

          case Some(s) if s == xsdDate =>
            SimpleDateTime.parseDate(maybeScalarValue.map(_.toString).getOrElse("")) match {
              case Left(_) => reportFailure(validationSpecification, propertyConstraint, parentElement.id)
              case _       =>
            }
          case Some(s) if s == xsdDateTime =>
            SimpleDateTime.parse(maybeScalarValue.map(_.toString).getOrElse("")) match {
              case Left(_) => reportFailure(validationSpecification, propertyConstraint, parentElement.id)
              case _       =>
            }
          case Some(s) if s == xsdDateTimeOnly =>
            SimpleDateTime.parseFullTime(maybeScalarValue.map(_.toString).getOrElse("")) match {
              case Left(_) => reportFailure(validationSpecification, propertyConstraint, parentElement.id)
              case _       =>
            }
          case Some(s) if s == xsdTime =>
            SimpleDateTime.parsePartialTime(maybeScalarValue.map(_.toString).getOrElse("")) match {
              case Left(_) => reportFailure(validationSpecification, propertyConstraint, parentElement.id)
              case _       =>
            }
          case Some(s) if s == xsdAnyURI =>
            validateURI(validationSpecification, propertyConstraint, parentElement.id, maybeScalarValue)
          case Some(other) =>
            throw new Exception(s"Data type '$other' for sh:datatype property constraint not supported yet")

          case _ => // ignore
        }
      case _ => // ignore
    }
  }

  /**
    * Check if argument is a valid URI, URL or URN
    */
  def validateURI(validationSpecification: ValidationSpecification,
                  propertyConstraint: PropertyConstraint,
                  id: String,
                  value: Option[Any]): Unit = {
    value.foreach { v =>
      try {
        v.toString.normalizePath
      } catch {
        case _: URISyntaxException =>
          reportFailure(validationSpecification, propertyConstraint, id)
      }
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

  private def reportFailure(validationSpecification: ValidationSpecification,
                            propertyConstraint: PropertyConstraint,
                            id: String): Unit = {
    reportBuilder.reportFailure(validationSpecification, propertyConstraint, id)
  }

  private def reportFailure(validationSpec: ValidationSpecification,
                            id: String,
                            path: String,
                            customMessage: Option[String] = None): Unit = {
    reportBuilder.reportFailure(validationSpec, id, path, customMessage)
  }
}
