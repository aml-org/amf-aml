package amf.plugins.features.validation

import amf.core.annotations.SourceAST
import amf.core.model.DataType
import amf.core.model.document.BaseUnit
import amf.core.model.domain._
import amf.core.parser.Annotations
import amf.core.services.RuntimeValidator.{CustomShaclFunction, CustomShaclFunctions, PropertyInfo}
import amf.core.services.ValidationOptions
import amf.core.validation.EffectiveValidations
import amf.core.validation.core._
import amf.core.vocabulary.Namespace
import amf.{MessageStyle, OASStyle, RAMLStyle}
import org.yaml.model.YScalar

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class CustomShaclValidator(model: BaseUnit,
                           validations: EffectiveValidations,
                           customFunctions: CustomShaclFunctions,
                           options: ValidationOptions)(implicit executionContext: ExecutionContext) {

  private val validationReport: CustomValidationReport = CustomValidationReport.empty

  def run: Future[ValidationReport] = {
    model.iterator().foreach {
      case e: DomainElement => validateIdentityTransformation(e)
      case _                =>
    }
    Future.successful(validationReport)
  }

  private def validateIdentityTransformation(element: DomainElement): Unit = {
    validations.effective.foreach {
      case (_, specification) =>
        val classes = element.meta.`type`.map(_.iri())
        if (matchingClass(specification, classes) || matchingInstance(specification, element)) {
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

  private def extractPropertyValue(propertyConstraint: PropertyConstraint,
                                   element: DomainElement): Option[(Annotations, AmfElement, Option[Any])] = {
    extractPredicateValue(propertyConstraint.ramlPropertyId, element)
  }

  private def extractPredicateValue(predicate: String,
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
        s"Arbitray SHACL validations not supported in custom SHACL validator: ${validationSpecification.id}")
  }

  private def validateFunctionConstraint(validationSpecification: ValidationSpecification,
                                         element: DomainElement): Unit = {
    val functionConstraint = validationSpecification.functionConstraint.get
    functionConstraint.internalFunction.foreach(name => {
      val validationFunction = getFunctionForName(name)
      // depending if propertyInfo is provided, violation is thrown at a given property, or by default on element
      val onViolation = (propertyInfo: Option[PropertyInfo]) =>
        propertyInfo match {
          case Some((_, field)) => reportFailure(validationSpecification, element.id, Some(field.toString))
          case _                => reportFailure(validationSpecification, element.id)
      }
      validationFunction(element, onViolation)
    })
  }

  private def getFunctionForName(name: String): CustomShaclFunction = customFunctions.get(name) match {
    case Some(validationFunction) => validationFunction
    case None =>
      throw new Exception(s"Custom function validations not supported in customm SHACL validator: $name")
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
              extractPredicateValue(targetObject, element) match {
                case Some((_, _: AmfScalar, Some(value: String))) =>
                  if (!value.contains("://")) {
                    reportFailure(validationSpecification, element.id)
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
    propertyConstraint.in match {
      case Nil                        => // ignore
      case Seq(_)                     => validateIn(validationSpecification, propertyConstraint, element)
      case _: mutable.WrappedArray[_] => validateIn(validationSpecification, propertyConstraint, element)
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
      case _   => throw new Exception(s"class property constraint not supported yet ${validationSpecification.id}")
    }
    if (propertyConstraint.custom.isDefined) {
      throw new Exception(s"custom property constraint not supported yet ${validationSpecification.id}")
    }
    if (propertyConstraint.customRdf.isDefined) {
      throw new Exception(s"customRdf property constraint not supported yet ${validationSpecification.id}")
    }
    if (propertyConstraint.multipleOf.isDefined) {
      throw new Exception(s"multipleOf property constraint not supported yet ${validationSpecification.id}")
    }
    if (propertyConstraint.patternedProperty.isDefined) {
      throw new Exception(s"patternedProperty property constraint not supported yet ${validationSpecification.id}")
    }
  }

  private def validatePropertyNode(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement): Unit = {
    if (propertyConstraint.node.get.endsWith("NonEmptyList")) {
      extractPropertyValue(propertyConstraint, parentElement) match {
        case Some((_, arr: AmfArray, _)) =>
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
      case Some((_, arr: AmfArray, _)) =>
        if (!(arr.values.length >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      // cases scalar and object are equals, but we need to match by specific class because in api designer
      // qax environment the match does not work with the trait amfElement class
      case Some((_, _: AmfScalar, _)) =>
        if (!(1 >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case Some((_, _: AmfObject, _)) =>
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

      case Some((_, arr: AmfArray, _)) =>
        if (!(arr.values.length <= propertyConstraint.maxCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case Some((_, _: AmfElement, _)) =>
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
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, _: AmfScalar, Some(value: String))) =>
        if (!(propertyConstraint.minLength.get.toInt <= value.length)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case Some((_, _: AmfScalar, Some(x)))
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
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, _: AmfScalar, Some(value: String))) =>
        if (!(propertyConstraint.maxLength.get.toInt > value.length)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case _ => // ignore
    }
  }

  private def validateIn(validationSpecification: ValidationSpecification,
                         propertyConstraint: PropertyConstraint,
                         parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, _: AmfScalar, Some(value: String))) =>
        if (!propertyConstraint.in.contains(value)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
        }

      case Some((_, arr: AmfArray, _)) =>
        arr.values.foreach {
          case scalar: AmfScalar =>
            if (!propertyConstraint.in.contains(scalar.value.asInstanceOf[String])) {
              reportFailure(validationSpecification, propertyConstraint, parentElement.id)
            }
          case _ => // ignore
        }

      case _ => // ignore
    }
  }

  private def validateMaxInclusive(validationSpecification: ValidationSpecification,
                                   propertyConstraint: PropertyConstraint,
                                   parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, _: AmfScalar, Some(value: Long))) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toLong >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Integer))) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toInt >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Float))) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toFloat >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Double))) =>
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
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, _: AmfScalar, Some(value: Long))) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toLong > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Integer))) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toInt > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Float))) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toFloat > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Double))) =>
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
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, _: AmfScalar, Some(value: Long))) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toLong <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Integer))) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toInt <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Float))) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toFloat <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Double))) =>
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
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, _: AmfScalar, Some(value: Long))) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toLong < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Integer))) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toInt < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Float))) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toFloat < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          }
        }

      case Some((_, _: AmfScalar, Some(value: Double))) =>
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
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, _: AmfScalar, Some(value))) =>
        if (valueDoesntComplyWithPattern(propertyConstraint, value))
          reportFailure(validationSpecification, propertyConstraint, parentElement.id)
      case Some((_, arr: AmfArray, _)) =>
        arr.values.foreach {
          case value: AmfScalar =>
            if (Option(value).isDefined && propertyConstraint.pattern.get.r.findFirstIn(value.toString).isEmpty)
              reportFailure(validationSpecification, propertyConstraint, parentElement.id)
          case _ => // ignore
        }
      case _ => // ignore
    }
  }

  private def valueDoesntComplyWithPattern(propertyConstraint: PropertyConstraint, value: Any) = {
    Option(value).isDefined && propertyConstraint.pattern.get.r.findFirstIn(value.toString).isEmpty
  }

  private def validateDataType(validationSpecification: ValidationSpecification,
                               propertyConstraint: PropertyConstraint,
                               parentElement: DomainElement): Unit = {
    val xsdString  = DataType.String
    val xsdBoolean = DataType.Boolean
    val xsdInteger = DataType.Integer
    val xsdDouble  = DataType.Double
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, element, _)) =>
        val elements = element match {
          case arr: AmfArray => arr.values
          case _             => Seq(element)
        }
        elements.foreach { element =>
          val maybeScalarValue = element match {
            case scalar: AmfScalar => Some(amfScalarToScala(scalar))
            case _                 => None
          }
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

            case Some(other) =>
              throw new Exception(s"Data type '$other' for sh:datatype property constraint not supported yet")

            case _ => // ignore
          }
        }
      case _ => // ignore
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

  private def reportFailure(validationSpecification: ValidationSpecification, id: String): Unit = {
    reportFailure(validationSpecification, id, "")
  }

  private def reportFailure(validationSpecification: ValidationSpecification,
                            propertyConstraint: PropertyConstraint,
                            id: String): Unit = {
    reportFailure(validationSpecification, id, propertyConstraint.ramlPropertyId)
  }

  private def reportFailure(validationSpecification: ValidationSpecification,
                            id: String,
                            propertyPath: Option[String] = None): Unit = {
    reportFailure(validationSpecification, id, propertyPath.getOrElse(""))
  }

  private def reportFailure(validationSpec: ValidationSpecification, id: String, path: String): Unit = {
    validationReport.registerResult(
        CustomValidationResult(
            message = getMessageOf(validationSpec, options.messageStyle),
            path = path,
            sourceConstraintComponent = validationSpec.id,
            focusNode = id,
            severity = ShaclSeverityUris.amfSeverity(validationSpec.severity),
            sourceShape = validationSpec.id
        ))
  }

  private def getMessageOf(validationSpec: ValidationSpecification, style: MessageStyle): Option[String] =
    style match {
      case RAMLStyle => validationSpec.ramlMessage.orElse(Some(validationSpec.message))
      case OASStyle  => validationSpec.oasMessage.orElse(Some(validationSpec.message))
      case _         => Some(validationSpec.message)
    }
}
