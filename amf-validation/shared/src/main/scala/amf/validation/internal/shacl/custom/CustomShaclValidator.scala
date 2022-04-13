package amf.validation.internal.shacl.custom

import amf.core.client.common.validation.MessageStyle
import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain._
import amf.core.client.scala.vocabulary.Namespace
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

import java.net.URISyntaxException

object CustomShaclValidator {

  case class ValidationInfo(field: Field, message: Option[String] = None, annotations: Option[Annotations] = None)
  trait CustomShaclFunction {
    val name: String
    // When no validation info is provided, the validation is thrown in domain element level
    def run(element: AmfObject, validate: Option[ValidationInfo] => Unit): Unit
  }
  type CustomShaclFunctions = Map[String, CustomShaclFunction]
}

class CustomShaclValidator(customFunctions: CustomShaclFunctions,
                           messageStyle: MessageStyle,
                           extractor: ElementExtractor = DefaultElementExtractor) {

  def validate(unit: BaseUnit, validations: Seq[ValidationSpecification]): ValidationReport = {
    val reportBuilder: ReportBuilder = new ReportBuilder(messageStyle)
    unit.iterator().foreach {
      case element: DomainElement => validateIdentityTransformation(element, validations, reportBuilder)
      case _                      =>
    }
    reportBuilder.build()
  }

  def validate(element: DomainElement, validations: Seq[ValidationSpecification]): ValidationReport = {
    val reportBuilder: ReportBuilder = new ReportBuilder(messageStyle)
    validateIdentityTransformation(element, validations, reportBuilder)
    reportBuilder.build()
  }

  def validateProperties(element: DomainElement, validations: Seq[ValidationSpecification]): ValidationReport = {
    val reportBuilder: ReportBuilder = new ReportBuilder(messageStyle)
    validations.foreach(validateProperty(element, _, reportBuilder))
    reportBuilder.build()
  }

  private def validateProperty(element: DomainElement,
                               validation: ValidationSpecification,
                               builder: ReportBuilder): Unit = {
    validation.propertyConstraints.foreach { propertyConstraint =>
      validatePropertyConstraint(validation, propertyConstraint, element, builder)
    }
  }

  private def validateIdentityTransformation(element: DomainElement,
                                             validations: Seq[ValidationSpecification],
                                             reportBuilder: ReportBuilder): Unit = {
    val classes        = element.meta.`type`.map(_.iri())
    val isExternalLink = element.isExternalLink.value()
    validations.foreach { specification =>
      if (!isExternalLink && (matchingClass(specification, classes) || matchingInstance(specification, element))) {
        validateElement(element, specification, reportBuilder)
      }
      validateObjectsOf(element, specification, reportBuilder)
    }
  }

  // this is always (?s sh:nodeKind sh:IRI), we still put the checking logic in place
  private def validateObjectsOf(element: DomainElement,
                                validationSpecification: ValidationSpecification,
                                reportBuilder: ReportBuilder): Unit = {
    validationSpecification.targetObject.foreach { property =>
      findFieldTarget(element, property) match {
        case Some((_: Annotations, objectsOf: AmfArray)) =>
          objectsOf.foreach {
            case obj: DomainElement =>
              validationSpecification.nodeConstraints.foreach { nodeConstraint =>
                validateNodeConstraint(validationSpecification, nodeConstraint, obj, reportBuilder)
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
    extractElement(property, element) match {
      case Some(value) =>
        value match {
          case elems: AmfArray   => Some((value.annotations, elems.values))
          case scalar: AmfScalar => Some((value.annotations, Seq(scalar)))
          case obj: AmfObject    => Some((value.annotations, Seq(obj)))
          case _                 => Some((value.annotations, Nil))
        }
      case _ => None
    }
  }

  private def extractElement(fieldUri: String, element: DomainElement): Option[AmfElement] =
    element.fields.getValueAsOption(fieldUri).map(_.value)

  private def validateElement(element: DomainElement,
                              validationSpecification: ValidationSpecification,
                              reportBuilder: ReportBuilder): Unit = {
    validationSpecification.closed match {
      case Some(_) => validateClosed(validationSpecification)
      case _       => // ignore
    }
    validationSpecification.custom match {
      case Some(_) => validateCustom(validationSpecification)
      case _       => // ignore
    }
    validationSpecification.functionConstraint match {
      case Some(_) => validateFunctionConstraint(validationSpecification, element, reportBuilder)
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
            element,
            reportBuilder
        )
      // Normal constraints here
      case _ => validateProperty(element, validationSpecification, reportBuilder)
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
                                         element: DomainElement,
                                         reportBuilder: ReportBuilder): Unit = {
    val functionConstraint = validationSpecification.functionConstraint.get
    functionConstraint.internalFunction.foreach(name => {
      val validationFunction = getFunctionForName(name)
      // depending if propertyInfo is provided, violation is thrown at a given property, or by default on element
      val onValidation = (validationInfo: Option[ValidationInfo]) =>
        validationInfo match {
          case Some(ValidationInfo(field, customMessage, _)) => // why annotations are never used?
            reportFailure(validationSpecification, element.id, field.toString, customMessage, reportBuilder)
          case _ => reportFailure(validationSpecification, element.id, "", reportBuilder = reportBuilder)
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
                                     element: DomainElement,
                                     reportBuilder: ReportBuilder): Unit = {
    val nodeKindIri = (Namespace.Shacl + "nodeKind").iri()
    val shaclIri    = (Namespace.Shacl + "IRI").iri()

    nodeConstraint.constraint match {
      case s if s == nodeKindIri =>
        nodeConstraint.value match {
          case v if v == shaclIri =>
            validationSpecification.targetObject.foreach { targetObject =>
              extractor.extractPlainPredicateValue(targetObject, element).foreach {
                case ExtractedPropertyValue(_: AmfScalar, Some(value: String)) if !value.contains("://") =>
                  reportFailure(validationSpecification, element.id, "", reportBuilder = reportBuilder)
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
                                         element: DomainElement,
                                         reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.node match {
      case Some(_) => validatePropertyNode(validationSpecification, propertyConstraint, element, reportBuilder)
      case _       =>
    }
    propertyConstraint.maxCount match {
      case Some(_) => validateMaxCount(validationSpecification, propertyConstraint, element, reportBuilder)
      case _       =>
    }
    propertyConstraint.minCount match {
      case Some(minCountValue) =>
        val minCount = minCountValue.toInt
        propertyConstraint.mandatory match {
          case Some(mandatoryValue) =>
            // If I have minCount and mandatory, I know I am in an array
            val mandatory = mandatoryValue == "true"
            // If minCount is 0 and it is mandatory, this comes from minItems = 0 + mandatory = true
            // I need to check only the presence of the property, empty arrays are valid
            if (minCount == 0 && mandatory)
              validateArrayPropertyLengthAndPresence(validationSpecification,
                                                     propertyConstraint,
                                                     element,
                                                     reportBuilder,
                                                     mustBePresent = mandatory)
            // If minCount is > 0 and it is not mandatory, this comes from minItems = n
            // I need to check only the length of the array, but only if it is present
            if (minCount > 0 && !mandatory)
              validateArrayPropertyLengthAndPresence(validationSpecification,
                                                     propertyConstraint,
                                                     element,
                                                     reportBuilder,
                                                     minItems = Some(minCount))
            // If minCount is > 0 and it is mandatory, this comes from minItems = n + mandatory = true
            // I need to check the presence and length of the array, the original constraint will handle it
            if (minCount > 0 && mandatory)
              validateArrayPropertyLengthAndPresence(validationSpecification,
                                                     propertyConstraint,
                                                     element,
                                                     reportBuilder,
                                                     mustBePresent = mandatory,
                                                     minItems = Some(minCount))
          case None =>
            // If there is no mandatory key, I run the original constraint
            validateMinCount(validationSpecification, propertyConstraint, element, reportBuilder)
        }
      case _ =>
    }
    propertyConstraint.maxLength match {
      case Some(_) => validateMaxLength(validationSpecification, propertyConstraint, element, reportBuilder)
      case _       =>
    }
    propertyConstraint.minLength match {
      case Some(_) => validateMinLength(validationSpecification, propertyConstraint, element, reportBuilder)
      case _       =>
    }
    propertyConstraint.in.toList match {
      case Nil => // ignore
      case _   => validateIn(validationSpecification, propertyConstraint, element, reportBuilder)
    }
    propertyConstraint.maxExclusive match {
      case Some(_) => validateMaxExclusive(validationSpecification, propertyConstraint, element, reportBuilder)
      case _       => // ignore
    }
    propertyConstraint.minExclusive match {
      case Some(_) => validateMinExclusive(validationSpecification, propertyConstraint, element, reportBuilder)
      case _       => // ignore
    }
    propertyConstraint.maxInclusive match {
      case Some(_) => validateMaxInclusive(validationSpecification, propertyConstraint, element, reportBuilder)
      case _       => // ignore
    }
    propertyConstraint.minInclusive match {
      case Some(_) => validateMinInclusive(validationSpecification, propertyConstraint, element, reportBuilder)
      case _       => // ignore
    }
    propertyConstraint.pattern match {
      case Some(_) => validatePattern(validationSpecification, propertyConstraint, element, reportBuilder)
      case _       => // ignore
    }
    propertyConstraint.datatype match {
      case Some(_) => validateDataType(validationSpecification, propertyConstraint, element, reportBuilder)
      case _       => // ignore
    }
    //
    // These are only used for payload validations
    //
    propertyConstraint.`class` match {
      case Nil => // ignore
      case _   => validateClass(validationSpecification, propertyConstraint, element, reportBuilder)
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
                            element: DomainElement,
                            reportBuilder: ReportBuilder): Unit = {
    extractor.extractPropertyValue(propertyConstraint, element).foreach {
      case ExtractedPropertyValue(obj: AmfObject, _) =>
        val current = obj.meta.`type`.map(_.iri())
        if (!propertyConstraint.`class`.exists(t => current.contains(t)))
          reportBuilder.reportFailure(validationSpecification, propertyConstraint, element.id)
      case _ => // ignore
    }
  }

  private def validatePropertyNode(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement,
                                   reportBuilder: ReportBuilder): Unit = {
    if (propertyConstraint.node.get.endsWith("NonEmptyList")) {
      extractor.extractPropertyValue(propertyConstraint, parentElement) match {
        case Some(ExtractedPropertyValue(arr: AmfArray, _)) =>
          if (arr.values.isEmpty) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }

        case _ => // ignore
      }
    } else {
      throw new Exception(s"Unsupported property node value ${propertyConstraint.node.get}")
    }
  }

  private def validateArrayPropertyLengthAndPresence(validationSpecification: ValidationSpecification,
                                                     propertyConstraint: PropertyConstraint,
                                                     parentElement: DomainElement,
                                                     reportBuilder: ReportBuilder,
                                                     mustBePresent: Boolean = false,
                                                     minItems: Option[Int] = None): Unit = {
    extractor.extractPropertyValue(propertyConstraint, parentElement) match {
      // The key is present and it is an array
      case Some(ExtractedPropertyValue(arr: AmfArray, _)) =>
        minItems match {
          case Some(minLength) =>
            // The key is present and I should check the array length
            if (!(arr.values.length >= minLength)) {
              reportFailure(validationSpecification.copy(message = s"Array must have a minimum of $minLength items"),
                            propertyConstraint,
                            parentElement.id,
                            reportBuilder)

//              reportFailure(validationSpecification,
//                            parentElement.id,
//                            "",
//                            Some(s"Array must have a minimum of $minLength items"),
//                            reportBuilder)
            }

          case None => // Only need to check that the key is present
        }
      case _ =>
        // The key is not present missing
        if (mustBePresent) reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
    }
  }

  private def validateMinCount(validationSpecification: ValidationSpecification,
                               propertyConstraint: PropertyConstraint,
                               parentElement: DomainElement,
                               reportBuilder: ReportBuilder): Unit = {
    extractor.extractPropertyValue(propertyConstraint, parentElement) match {
      case Some(ExtractedPropertyValue(arr: AmfArray, _)) =>
        if (!(arr.values.length >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
        }

      // cases scalar and object are equals, but we need to match by specific class because in api designer
      // qax environment the match does not work with the trait amfElement class
      case Some(ExtractedPropertyValue(_: AmfScalar, _)) =>
        if (!(1 >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
        }

      case Some(ExtractedPropertyValue(_: AmfObject, _)) =>
        if (!(1 >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
        }

      case _ =>
        if (!propertyConstraint.minCount.contains("0"))
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
    }
  }

  private def validateMaxCount(validationSpecification: ValidationSpecification,
                               propertyConstraint: PropertyConstraint,
                               parentElement: DomainElement,
                               reportBuilder: ReportBuilder): Unit = {
    extractor.extractPropertyValue(propertyConstraint, parentElement) match {

      case Some(ExtractedPropertyValue(arr: AmfArray, _)) =>
        if (!(arr.values.length <= propertyConstraint.maxCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
        }

      case Some(ExtractedPropertyValue(_: AmfElement, _)) =>
        if (!(1 <= propertyConstraint.maxCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
        }

      case _ =>
      // ignore
    }
  }

  private def validateMinLength(validationSpecification: ValidationSpecification,
                                propertyConstraint: PropertyConstraint,
                                parentElement: DomainElement,
                                reportBuilder: ReportBuilder): Unit = {
    extractor.extractPlainPropertyValue(propertyConstraint, parentElement) match {
      case Seq(ExtractedPropertyValue(_: AmfScalar, Some(value: String))) =>
        if (!(propertyConstraint.minLength.get.toInt <= value.length)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
        }

      case Seq(ExtractedPropertyValue(_: AmfScalar, Some(x)))
          if Option(x).isEmpty => // this happens in cases where the value of a key in YAML is the empty string
        if (!(propertyConstraint.minLength.get.toInt <= 0)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
        }

      case _ => // ignore
    }
  }

  private def validateMaxLength(validationSpecification: ValidationSpecification,
                                propertyConstraint: PropertyConstraint,
                                parentElement: DomainElement,
                                reportBuilder: ReportBuilder): Unit = {
    extractor.extractPlainPropertyValue(propertyConstraint, parentElement) match {
      case Seq(ExtractedPropertyValue(_: AmfScalar, Some(value: String))) =>
        if (!(propertyConstraint.maxLength.get.toInt > value.length)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
        }

      case _ => // ignore
    }
  }

  private def validateIn(validationSpecification: ValidationSpecification,
                         propertyConstraint: PropertyConstraint,
                         parentElement: DomainElement,
                         reportBuilder: ReportBuilder): Unit = {
    extractor.extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, Some(value: Any)) =>
        if (!propertyConstraint.in.contains(value)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
        }

      case _ => // ignore
    }
  }

  private def validateMaxInclusive(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement,
                                   reportBuilder: ReportBuilder): Unit = {
    extractor.extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, Some(value: Long)) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toLong >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Integer)) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toInt >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Float)) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toFloat >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Double)) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toFloat >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case _ => // ignore
    }
  }

  private def validateMaxExclusive(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement,
                                   reportBuilder: ReportBuilder): Unit = {
    extractor.extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, Some(value: Long)) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toLong > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Integer)) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toInt > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Float)) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toFloat > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Double)) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toFloat > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case _ => // ignore
    }
  }

  private def validateMinInclusive(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement,
                                   reportBuilder: ReportBuilder): Unit = {
    extractor.extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, Some(value: Long)) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toLong <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Integer)) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toInt <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Float)) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toFloat <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Double)) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toFloat <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case _ => // ignore
    }
  }

  private def validateMinExclusive(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement,
                                   reportBuilder: ReportBuilder): Unit = {
    extractor.extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, Some(value: Long)) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toLong < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Integer)) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toInt < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Float)) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toFloat < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case ExtractedPropertyValue(_: AmfScalar, Some(value: Double)) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toFloat < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
          }
        }

      case _ => // ignore
    }
  }

  private def validatePattern(validationSpecification: ValidationSpecification,
                              propertyConstraint: PropertyConstraint,
                              parentElement: DomainElement,
                              reportBuilder: ReportBuilder): Unit = {
    extractor.extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(scalar: AmfScalar, _) =>
        if (valueDoesntComplyWithPattern(propertyConstraint, scalar))
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
      case _ => // ignore
    }
  }

  private def valueDoesntComplyWithPattern(propertyConstraint: PropertyConstraint, value: AmfScalar) = {
    Option(value).isDefined && propertyConstraint.pattern.get.r.findFirstIn(value.toString).isEmpty
  }

  private def validateDataType(validationSpecification: ValidationSpecification,
                               propertyConstraint: PropertyConstraint,
                               parentElement: DomainElement,
                               reportBuilder: ReportBuilder): Unit = {
    val xsdString       = DataType.String
    val xsdBoolean      = DataType.Boolean
    val xsdInteger      = DataType.Integer
    val xsdDouble       = DataType.Double
    val xsdDate         = DataType.Date
    val xsdDateTime     = DataType.DateTime
    val xsdDateTimeOnly = DataType.DateTimeOnly
    val xsdTime         = DataType.Time
    val xsdAnyURI       = DataType.AnyUri

    extractor.extractPlainPropertyValue(propertyConstraint, parentElement).foreach {
      case ExtractedPropertyValue(_: AmfScalar, maybeScalarValue) =>
        propertyConstraint.datatype match {
          case Some(s) if s == xsdString => // ignore

          case Some(s) if s == xsdBoolean =>
            maybeScalarValue match {
              case Some(_: Boolean) => // ignore
              case _ =>
                reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
            }
          case Some(s) if s == xsdInteger =>
            maybeScalarValue match {
              case Some(_: Integer) => // ignore
              case Some(_: Long)    => // ignore
              case _ =>
                reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
            }

          case Some(s) if s == xsdDouble =>
            maybeScalarValue match {
              case Some(_: Integer) => // ignore
              case Some(_: Long)    => // ignore
              case Some(_: Double)  => // ignore
              case Some(_: Float)   => // ignore
              case _ =>
                reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
            }

          case Some(s) if s == xsdDate =>
            SimpleDateTime.parseDate(maybeScalarValue.map(_.toString).getOrElse("")) match {
              case Left(_) =>
                reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
              case _ =>
            }
          case Some(s) if s == xsdDateTime =>
            SimpleDateTime.parse(maybeScalarValue.map(_.toString).getOrElse("")) match {
              case Left(_) =>
                reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
              case _ =>
            }
          case Some(s) if s == xsdDateTimeOnly =>
            SimpleDateTime.parseFullTime(maybeScalarValue.map(_.toString).getOrElse("")) match {
              case Left(_) =>
                reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
              case _ =>
            }
          case Some(s) if s == xsdTime =>
            SimpleDateTime.parsePartialTime(maybeScalarValue.map(_.toString).getOrElse("")) match {
              case Left(_) =>
                reportFailure(validationSpecification, propertyConstraint, parentElement.id, reportBuilder)
              case _ =>
            }
          case Some(s) if s == xsdAnyURI =>
            validateURI(validationSpecification, propertyConstraint, parentElement.id, maybeScalarValue, reportBuilder)
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
                  value: Option[Any],
                  reportBuilder: ReportBuilder): Unit = {
    value.foreach { v =>
      try {
        v.toString.normalizePath
      } catch {
        case _: URISyntaxException =>
          reportFailure(validationSpecification, propertyConstraint, id, reportBuilder)
      }
    }
  }

  private def reportFailure(validationSpecification: ValidationSpecification,
                            propertyConstraint: PropertyConstraint,
                            id: String,
                            reportBuilder: ReportBuilder): Unit = {
    reportBuilder.reportFailure(validationSpecification, propertyConstraint, id)
  }

  private def reportFailure(validationSpec: ValidationSpecification,
                            id: String,
                            path: String,
                            customMessage: Option[String] = None,
                            reportBuilder: ReportBuilder): Unit = {
    reportBuilder.reportFailure(validationSpec, id, path, customMessage)
  }
}
