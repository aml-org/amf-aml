package amf.plugins.validation

import java.util.regex.Pattern

import amf.core.annotations.SourceAST
import amf.core.metamodel.DynamicObj
import amf.core.model.document.BaseUnit
import amf.core.model.domain._
import amf.core.parser.Annotations
import amf.core.services.ValidationOptions
import amf.core.utils.RegexConverter
import amf.core.validation.core._
import amf.core.validation.{EffectiveValidations, SeverityLevels}
import amf.core.vocabulary.Namespace
import amf.{OASStyle, RAMLStyle}
import org.yaml.model.YScalar

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class CustomValidationResult(message: Option[String],
                                  path: String,
                                  sourceConstraintComponent: String,
                                  focusNode: String,
                                  severity: String,
                                  sourceShape: String)
    extends ValidationResult

class CustomValidationReport(var rs: List[ValidationResult] = Nil) extends ValidationReport {

  val duplicates: mutable.Set[String] = mutable.Set()

  override def conforms: Boolean = results.exists(_.severity == SeverityLevels.VIOLATION)

  override def results: List[ValidationResult] = rs

  def registerResult(result: ValidationResult): Unit = {
    val key = result.sourceShape + result.sourceConstraintComponent + result.focusNode
    if (!duplicates.contains(key)) {
      duplicates += key
      rs ++= Seq(result)
    }
  }
}

class CustomShaclValidator(model: BaseUnit, validations: EffectiveValidations, options: ValidationOptions) {

  var validationReport: CustomValidationReport = new CustomValidationReport(Nil)

  def run: Future[ValidationReport] = {
    model.findBy(elementToValidateSelector).foreach { found =>
      validateIdentityTransformation(found)
    }
    Future(validationReport)
  }

  protected def elementToValidateSelector(element: DomainElement): Boolean = {
    true
  }

  protected def validateIdentityTransformation(element: DomainElement): Unit = {
    validations.effective.foreach {
      case (_, validationSpecification) =>
        if (selectedNode(validationSpecification, element)) {
          validate(validationSpecification, element)
        }
        validateObjectsOf(validationSpecification, element)
    }
  }

  // this is always (?s sh:nodeKind sh:IRI), we still put the checking logic in place
  protected def validateObjectsOf(validationSpecification: ValidationSpecification, element: DomainElement): Unit = {
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

  protected def selectedNode(validationSpecification: ValidationSpecification, element: DomainElement): Boolean =
    matchingClass(validationSpecification, element) || matchingInstance(validationSpecification, element)

  protected def matchingClass(validationSpecification: ValidationSpecification, element: DomainElement): Boolean = {
    val classes = element.meta.`type`.map(_.iri())
    validationSpecification.targetClass.exists { cls =>
      classes.contains(cls)
    }
  }

  protected def matchingInstance(validationSpecification: ValidationSpecification, element: DomainElement): Boolean =
    validationSpecification.targetInstance.contains(element.id)

  def findFieldTarget(element: DomainElement, property: String): Option[(Annotations, Seq[AmfElement])] = {
    if (element.meta.isInstanceOf[DynamicObj] && element.isInstanceOf[DynamicDomainElement]) {
      val dynamicElement = element.asInstanceOf[DynamicDomainElement]
      dynamicElement.meta.fields.find(_.value.iri() == property) match {
        case Some(field) =>
          dynamicElement.valueForField(field) match {
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

    } else {
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
  }

  def extractPropertyValue(propertyConstraint: PropertyConstraint,
                           element: DomainElement): Option[(Annotations, AmfElement, Option[Any])] = {
    extractPredicateValue(propertyConstraint.ramlPropertyId, element)
  }

  def extractPredicateValue(predicate: String,
                            element: DomainElement): Option[(Annotations, AmfElement, Option[Any])] = {
    if (element.meta.isInstanceOf[DynamicObj] && element.isInstanceOf[DynamicDomainElement]) {
      val dynamicDomainElement = element.asInstanceOf[DynamicDomainElement]
      dynamicDomainElement.meta.fields.find { f =>
        f.value.iri() == predicate
      } match {
        case None =>
          None
        case Some(f) =>
          dynamicDomainElement.valueForField(f) match {
            case Some(value) if value.value.isInstanceOf[AmfScalar] =>
              Some((value.annotations, value.value, Some(amfScalarToScala(value.value.asInstanceOf[AmfScalar]))))
            case Some(value) =>
              Some((value.annotations, value.value, None))
            case _ => None
          }
      }
    } else {
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
  }

  def validate(validationSpecification: ValidationSpecification, element: DomainElement): Unit = {
    validationSpecification.closed match {
      case Some(closed) => validateClosed(validationSpecification, element, closed)
      case _            => // ignore
    }
    validationSpecification.custom match {
      case Some(_) => validateCustom(validationSpecification, element)
      case _       => // ignore
    }
    validationSpecification.functionConstraint match {
      case Some(_) => validateFunctionConstraint(validationSpecification, element)
      case _       => // ignore
    }
    validationSpecification.propertyConstraints.foreach { propertyConstraint =>
      validatePropertyConstraint(validationSpecification, propertyConstraint, element)
    }

  }

  def validateClosed(validationSpecification: ValidationSpecification, element: DomainElement, closed: Boolean): Unit = {
    throw new Exception(s"Closed constraint not supported yet: ${validationSpecification.id}")
  }

  def validateCustom(validationSpecification: ValidationSpecification, element: DomainElement): Unit = {
    throw new Exception(
      s"Arbitray SHACL validations not supported in customm SHACL validator: ${validationSpecification.id}")
  }

  def validateFunctionConstraint(validationSpecification: ValidationSpecification, element: DomainElement): Unit = {
    val functionConstraint = validationSpecification.functionConstraint.get
    functionConstraint.internalFunction match {
      case Some("minimumMaximumValidation") =>
        val maybeMinInclusive = element.fields.fields().find { f =>
          f.field.value.iri().endsWith("minInclusive")
        } match {
          case Some(f) if f.value.value.isInstanceOf[AmfScalar] => Some(f.value.value.asInstanceOf[AmfScalar])
          case _                                                => None
        }

        val maybeMaxInclusive = element.fields.fields().find { f =>
          f.field.value.iri().endsWith("maxInclusive")
        } match {
          case Some(f) if f.value.value.isInstanceOf[AmfScalar] => Some(f.value.value.asInstanceOf[AmfScalar])
          case _                                                => None
        }

        if (maybeMaxInclusive.nonEmpty && maybeMinInclusive.nonEmpty) {
          val minInclusive = maybeMinInclusive.get.value.toString.toDouble
          val maxInclusive = maybeMaxInclusive.get.value.toString.toDouble
          if (minInclusive > maxInclusive) {
            reportFailure(validationSpecification, functionConstraint, element.id, element.annotations)
          }
        }

      case Some("pathParameterRequiredProperty") =>
        val optBindingValue = element.fields
          .fields()
          .find { f =>
            f.field.value.iri().endsWith("binding")
          }
          .map(field => field.value.value)
          .collect { case AmfScalar(value, _) => value }

        val optRequiredValue = element.fields
          .fields()
          .find { f =>
            f.field.value.iri().endsWith("required")
          }
          .map(field => field.value.value)
          .collect { case AmfScalar(value, _) => value }

        (optBindingValue, optRequiredValue) match {
          case (Some("path"), Some(false)) | (Some("path"), None) =>
            reportFailure(validationSpecification, functionConstraint, element.id, element.annotations)
          case _ =>
        }

      case Some("minMaxItemsValidation") =>
        val maybeMinInclusive = element.fields.fields().find { f =>
          f.field.value.iri().endsWith("minCount")
        } match {
          case Some(f) if f.value.value.isInstanceOf[AmfScalar] => Some(f.value.value.asInstanceOf[AmfScalar])
          case _                                                => None
        }

        val maybeMaxInclusive = element.fields.fields().find { f =>
          f.field.value.iri().endsWith("maxCount")
        } match {
          case Some(f) if f.value.value.isInstanceOf[AmfScalar] => Some(f.value.value.asInstanceOf[AmfScalar])
          case _                                                => None
        }

        if (maybeMaxInclusive.nonEmpty && maybeMinInclusive.nonEmpty) {
          val minInclusive = maybeMinInclusive.get.value.toString.toDouble
          val maxInclusive = maybeMaxInclusive.get.value.toString.toDouble
          if (minInclusive > maxInclusive) {
            reportFailure(validationSpecification, functionConstraint, element.id, element.annotations)
          }
        }
      case Some("minMaxPropertiesValidation") =>
        val maybeMinInclusive = element.fields.fields().find { f =>
          f.field.value.iri().endsWith("minProperties")
        } match {
          case Some(f) if f.value.value.isInstanceOf[AmfScalar] => Some(f.value.value.asInstanceOf[AmfScalar])
          case _                                                => None
        }

        val maybeMaxInclusive = element.fields.fields().find { f =>
          f.field.value.iri().endsWith("maxProperties")
        } match {
          case Some(f) if f.value.value.isInstanceOf[AmfScalar] => Some(f.value.value.asInstanceOf[AmfScalar])
          case _                                                => None
        }

        if (maybeMaxInclusive.nonEmpty && maybeMinInclusive.nonEmpty) {
          val minInclusive = maybeMinInclusive.get.toString.toDouble
          val maxInclusive = maybeMaxInclusive.get.toString.toDouble
          if (minInclusive > maxInclusive) {
            reportFailure(validationSpecification, functionConstraint, element.id, element.annotations)
          }
        }

      case Some("minMaxLengthValidation") =>
        val maybeMinInclusive = element.fields.fields().find { f =>
          f.field.value.iri().endsWith("minLength")
        } match {
          case Some(f) if f.value.value.isInstanceOf[AmfScalar] => Some(f.value.value.asInstanceOf[AmfScalar])
          case _                                                => None
        }

        val maybeMaxInclusive = element.fields.fields().find { f =>
          f.field.value.iri().endsWith("maxLength")
        } match {
          case Some(f) if f.value.value.isInstanceOf[AmfScalar] => Some(f.value.value.asInstanceOf[AmfScalar])
          case _                                                => None
        }

        if (maybeMaxInclusive.nonEmpty && maybeMinInclusive.nonEmpty) {
          val minInclusive = maybeMinInclusive.get.value.toString.toDouble
          val maxInclusive = maybeMaxInclusive.get.value.toString.toDouble
          if (minInclusive > maxInclusive) {
            reportFailure(validationSpecification, functionConstraint, element.id, element.annotations)
          }
        }

      case Some("xmlWrappedScalar") =>
        val isScalar = element.meta.`type`.exists(_.name == "ScalarShape")
        if (isScalar) {
          element.fields.fields().find { f =>
            f.field.value.iri().endsWith("xmlSerialization")
          } match {
            case Some(f) =>
              val xmlSerialization = f.value.value.asInstanceOf[DomainElement]
              xmlSerialization.fields
                .fields()
                .find(f => f.field.value.iri().endsWith("xmlWrapped"))
                .foreach { isWrappedEntry =>
                  val isWrapped = isWrappedEntry.scalar.toBool
                  if (isWrapped) {
                    reportFailure(validationSpecification, functionConstraint, element.id, element.annotations)
                  }
                }
            case None => // Nothing
          }
        }

      case Some("xmlNonScalarAttribute") =>
        element.fields.fields().find { f =>
          f.field.value.iri().endsWith("xmlSerialization")
        } match {
          case Some(f) =>
            val xmlSerialization = f.value.value.asInstanceOf[DomainElement]
            xmlSerialization.fields
              .fields()
              .find(f => f.field.value.iri().endsWith("xmlAttribute"))
              .foreach { isAttributeEntry =>
                val isAttribute = isAttributeEntry.scalar.toBool
                val isNonScalar = !element.meta.`type`.exists(_.name == "ScalarShape")
                if (isAttribute && isNonScalar) {
                  reportFailure(validationSpecification, functionConstraint, element.id, element.annotations)
                }
              }
          case None => // Nothing
        }

      case Some("patternValidation") =>
        element.fields
          .fields()
          .find(_.field.value.iri().endsWith("pattern"))
          .map(_.value.value.asInstanceOf[AmfScalar].toString)
          .foreach { pattern =>
            try Pattern.compile(pattern.convertRegex)
            catch {
              case _: Throwable =>
                reportFailure(validationSpecification, functionConstraint, element.id, element.annotations)
            }
          }

      case Some(other) =>
        throw new Exception(s"Custom function validations not supported in customm SHACL validator: $other")
      case _ =>
        throw new Exception(
          s"Custom function validations not supported in customm SHACL validator: ${validationSpecification.id}")
    }
  }

  def validateNodeConstraint(validationSpecification: ValidationSpecification,
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
                case Some((_, scalar: AmfScalar, Some(value: String))) =>
                  if (!value.contains("://")) {
                    reportFailure(validationSpecification, element.id, scalar.annotations)
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

  def validatePropertyConstraint(validationSpecification: ValidationSpecification,
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

  def validatePropertyNode(validationSpecification: ValidationSpecification,
                           propertyConstraint: PropertyConstraint,
                           parentElement: DomainElement): Unit = {
    if (propertyConstraint.node.get.endsWith("NonEmptyList")) {
      extractPropertyValue(propertyConstraint, parentElement) match {
        case Some((_, arr: AmfArray, _)) =>
          if (arr.values.isEmpty) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, arr.annotations)
          }

        case _ => // ignore
      }
    } else {
      throw new Exception(s"Unsupported property node value ${propertyConstraint.node.get}")
    }
  }

  def validateMinCount(validationSpecification: ValidationSpecification,
                       propertyConstraint: PropertyConstraint,
                       parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, arr: AmfArray, _)) =>
        if (!(arr.values.length >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, arr.annotations)
        }

      // cases scalar and object are equals, but we need to match by specific class because in api designer
      // qax environment the match does not work with the trait amfElement class
      case Some((_, x: AmfScalar, _)) =>
        if (!(1 >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, x.annotations)
        }

      case Some((_, x: AmfObject, _)) =>
        if (!(1 >= propertyConstraint.minCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, x.annotations)
        }

      case _ =>
        if (!propertyConstraint.minCount.contains("0"))
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, parentElement.annotations)
    }
  }

  def validateMaxCount(validationSpecification: ValidationSpecification,
                       propertyConstraint: PropertyConstraint,
                       parentElement: DomainElement): Unit = {
    /*
    extractPropertyValue(propertyConstraint, parentElement) match {

      case Some((_, arr: AmfArray, _))  =>
        if (! (arr.values.length <= propertyConstraint.maxCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, arr.annotations)
        }


      case Some((_, x: AmfElement, _))  =>
        if (! (1 <= propertyConstraint.maxCount.get.toInt)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, x.annotations)
        }

      case _ =>
        // ignore
    }
   */
  }

  def validateMinLength(validationSpecification: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, scalar: AmfScalar, Some(value: String))) =>
        if (!(propertyConstraint.minLength.get.toInt <= value.length)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
        }

      case Some((_, scalar: AmfScalar, Some(x)))
          if Option(x).isEmpty => // this happens in cases where the value of a key in YAML is the empty string
        if (!(propertyConstraint.minLength.get.toInt <= 0)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
        }

      case _ => // ignore
    }
  }

  def validateMaxLength(validationSpecification: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, scalar: AmfScalar, Some(value: String))) =>
        if (!(propertyConstraint.maxLength.get.toInt > value.length)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
        }

      case _ => // ignore
    }
  }

  def validateIn(validationSpecification: ValidationSpecification,
                 propertyConstraint: PropertyConstraint,
                 parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, _: AmfScalar, Some(value: String))) =>
        if (!propertyConstraint.in.contains(value)) {
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, parentElement.annotations)
        }

      case Some((_, arr: AmfArray, _)) =>
        arr.values.foreach {
          case scalar: AmfScalar =>
            if (!propertyConstraint.in.contains(scalar.value.asInstanceOf[String])) {
              reportFailure(validationSpecification, propertyConstraint, parentElement.id, parentElement.annotations)
            }
          case _ => // ignore
        }

      case _ => // ignore
    }
  }

  def validateMaxInclusive(validationSpecification: ValidationSpecification,
                           propertyConstraint: PropertyConstraint,
                           parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, scalar: AmfScalar, Some(value: Long))) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toLong >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Integer))) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toInt >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Float))) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toFloat >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Double))) =>
        if (propertyConstraint.maxInclusive.get.contains(".")) {
          if (!(propertyConstraint.maxInclusive.get.toDouble >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.maxInclusive.get.toFloat >= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case _ => // ignore
    }
  }

  def validateMaxExclusive(validationSpecification: ValidationSpecification,
                           propertyConstraint: PropertyConstraint,
                           parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, scalar: AmfScalar, Some(value: Long))) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toLong > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Integer))) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toInt > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Float))) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toFloat > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Double))) =>
        if (propertyConstraint.maxExclusive.get.contains(".")) {
          if (!(propertyConstraint.maxExclusive.get.toDouble > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.maxExclusive.get.toFloat > value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case _ => // ignore
    }
  }

  def validateMinInclusive(validationSpecification: ValidationSpecification,
                           propertyConstraint: PropertyConstraint,
                           parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, scalar: AmfScalar, Some(value: Long))) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toLong <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Integer))) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toInt <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Float))) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toFloat <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Double))) =>
        if (propertyConstraint.minInclusive.get.contains(".")) {
          if (!(propertyConstraint.minInclusive.get.toDouble <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.minInclusive.get.toFloat <= value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case _ => // ignore
    }
  }

  def validateMinExclusive(validationSpecification: ValidationSpecification,
                           propertyConstraint: PropertyConstraint,
                           parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, scalar: AmfScalar, Some(value: Long))) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toLong < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Integer))) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toInt < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Float))) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value.toDouble)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toFloat < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case Some((_, scalar: AmfScalar, Some(value: Double))) =>
        if (propertyConstraint.minExclusive.get.contains(".")) {
          if (!(propertyConstraint.minExclusive.get.toDouble < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        } else {
          if (!(propertyConstraint.minExclusive.get.toFloat < value)) {
            reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
          }
        }

      case _ => // ignore
    }
  }

  def validatePattern(validationSpecification: ValidationSpecification,
                      propertyConstraint: PropertyConstraint,
                      parentElement: DomainElement): Unit = {
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, scalar: AmfScalar, Some(value))) =>
        if (Option(value).isDefined && propertyConstraint.pattern.get.r.findFirstIn(value.toString).isEmpty)
          reportFailure(validationSpecification, propertyConstraint, parentElement.id, scalar.annotations)
      case Some((_, arr: AmfArray, _)) =>
        arr.values.foreach {
          case value: AmfScalar =>
            if (Option(value).isDefined && propertyConstraint.pattern.get.r.findFirstIn(value.toString).isEmpty)
              reportFailure(validationSpecification, propertyConstraint, parentElement.id, value.annotations)
          case _ => // ignore
        }
      case _ => // ignore
    }
  }

  def validateDataType(validationSpecification: ValidationSpecification,
                       propertyConstraint: PropertyConstraint,
                       parentElement: DomainElement): Unit = {
    val xsdString  = (Namespace.Xsd + "string").iri()
    val xsdBoolean = (Namespace.Xsd + "boolean").iri()
    val xsdInteger = (Namespace.Xsd + "integer").iri()
    val xsdDouble  = (Namespace.Xsd + "double").iri()
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
            case Some(s) if s == xsdString =>
            /*
              maybeScalarValue match {
                case Some(_ : String) => // ignore
                case Some(_) // exception
                  if propertyConstraint.name == "http://a.ml/vocabularies/amf/parser#WebAPI-version-datatype/prop" => // ignore
                case _                =>
                  reportFailure(validationSpecification,
                    propertyConstraint,
                    parentElement.id,
                    element.asInstanceOf[AmfScalar].annotations)
              }
             */
            case Some(s) if s == xsdBoolean =>
              maybeScalarValue match {
                case Some(_: Boolean) => // ignore
                case _ =>
                  reportFailure(validationSpecification,
                                propertyConstraint,
                                parentElement.id,
                                element.asInstanceOf[AmfScalar].annotations)
              }
            case Some(s) if s == xsdInteger =>
              maybeScalarValue match {
                case Some(_: Integer) => // ignore
                case Some(_: Long)    => // ignore
                case _ =>
                  reportFailure(validationSpecification,
                                propertyConstraint,
                                parentElement.id,
                                element.asInstanceOf[AmfScalar].annotations)
              }

            case Some(s) if s == xsdDouble =>
              maybeScalarValue match {
                case Some(_: Integer) => // ignore
                case Some(_: Long)    => // ignore
                case Some(_: Double)  => // ignore
                case Some(_: Float)   => // ignore
                case _ =>
                  reportFailure(validationSpecification,
                                propertyConstraint,
                                parentElement.id,
                                element.asInstanceOf[AmfScalar].annotations)
              }

            case Some(other) =>
              throw new Exception(s"Data type '$other' for sh:datatype property constraint not supported yet")

            case _ => // ignore
          }
        }
      case _ => // ignore
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

  def reportFailure(validationSpecification: ValidationSpecification, id: String, annotations: Annotations): Unit = {
    validationReport.registerResult(
      CustomValidationResult(
        message = options.messageStyle match {
          case RAMLStyle => validationSpecification.ramlMessage.orElse(Some(validationSpecification.message))
          case OASStyle  => validationSpecification.oasMessage.orElse(Some(validationSpecification.message))
          case _         => Some(validationSpecification.message)
        },
        path = "",
        sourceConstraintComponent = validationSpecification.id,
        focusNode = id,
        severity = SeverityLevels.VIOLATION,
        sourceShape = validationSpecification.id
      ))
  }

  def reportFailure(validationSpecification: ValidationSpecification,
                    propertyConstraint: PropertyConstraint,
                    id: String,
                    annotations: Annotations): Unit = {
    validationReport.registerResult(
      CustomValidationResult(
        message = options.messageStyle match {
          case RAMLStyle => validationSpecification.ramlMessage.orElse(Some(validationSpecification.message))
          case OASStyle  => validationSpecification.oasMessage.orElse(Some(validationSpecification.message))
          case _         => Some(validationSpecification.message)
        },
        path = propertyConstraint.ramlPropertyId,
        sourceConstraintComponent = validationSpecification.id,
        focusNode = id,
        severity = SeverityLevels.VIOLATION,
        sourceShape = validationSpecification.id
      ))
  }

  def reportFailure(validationSpecification: ValidationSpecification,
                    functionConstraint: FunctionConstraint,
                    id: String,
                    annotations: Annotations): Unit = {
    validationReport.registerResult(
      CustomValidationResult(
        message = options.messageStyle match {
          case RAMLStyle => validationSpecification.ramlMessage.orElse(Some(validationSpecification.message))
          case OASStyle  => validationSpecification.oasMessage.orElse(Some(validationSpecification.message))
          case _         => Some(validationSpecification.message)
        },
        path = "",
        sourceConstraintComponent = validationSpecification.id,
        focusNode = id,
        severity = SeverityLevels.VIOLATION,
        sourceShape = validationSpecification.id
      ))
  }

}
