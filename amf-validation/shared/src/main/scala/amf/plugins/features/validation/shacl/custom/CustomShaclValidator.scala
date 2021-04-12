package amf.plugins.features.validation.shacl.custom

import amf.core.model.document.BaseUnit
import amf.core.model.domain._
import amf.core.services.RuntimeValidator.{CustomShaclFunction, CustomShaclFunctions, PropertyInfo}
import amf.core.services.ValidationOptions
import amf.core.validation.EffectiveValidations
import amf.core.validation.core._
import amf.plugins.features.validation.shacl.custom.CustomShaclValidator.PROPERTY_CONSTRAINT_VALIDATORS
import amf.plugins.features.validation.shacl.custom.validators._

import scala.concurrent.{ExecutionContext, Future}

object CustomShaclValidator {

  protected val PROPERTY_CONSTRAINT_VALIDATORS = Seq(
      PropertyNodeConstraint,
      MaxCountConstraint,
      MinCountConstraint,
      MaxLengthConstraint,
      MinLengthConstraint,
      InConstraint,
      MaxExclusiveConstraint,
      MinExclusiveConstraint,
      MaxInclusiveConstraint,
      MinInclusiveConstraint,
      PatternConstraint,
      DataTypeConstraint,
      UnsupportedPropertyConstraint.`class`,
      UnsupportedPropertyConstraint.customProperty,
      UnsupportedPropertyConstraint.customRdf,
      UnsupportedPropertyConstraint.multipleOf,
      UnsupportedPropertyConstraint.patternedProperty
  )
}

class CustomShaclValidator(model: BaseUnit,
                           validations: Seq[ValidationSpecification],
                           customFunctions: CustomShaclFunctions,
                           options: ValidationOptions)(implicit executionContext: ExecutionContext) {

  private val reportBuilder: ReportBuilder = new ReportBuilder(options)

  def run: Future[ValidationReport] = {
    model.iterator().foreach {
      case e: DomainElement => validateIdentityTransformation(e)
      case _                =>
    }
    Future.successful(reportBuilder.build())
  }

  private def validateIdentityTransformation(element: DomainElement): Unit = {
    validations.foreach { specification =>
      if (matchesNode(specification, element)) validate(specification, element)
      TargetObjectsOfConstraint.validate(specification, element, reportBuilder)
    }
  }

  private def matchesNode(specification: ValidationSpecification, element: DomainElement) = {
    matchesClass(specification, element) || matchesInstance(specification, element)
  }

  private def metaClassIris(element: DomainElement) = element.meta.`type`.map(_.iri())

  private def matchesClass(specification: ValidationSpecification, element: DomainElement): Boolean = {
    val classes = metaClassIris(element)
    specification.targetClass.exists(classes.contains)
  }

  private def matchesInstance(specification: ValidationSpecification, element: DomainElement): Boolean =
    specification.targetInstance.contains(element.id)

  private def validate(validationSpecification: ValidationSpecification, element: DomainElement): Unit = {
    UnsupportedConstraint.closed.validate(validationSpecification, element, reportBuilder)
    UnsupportedConstraint.custom.validate(validationSpecification, element, reportBuilder)

    validationSpecification.functionConstraint match {
      case Some(_) => validateFunctionConstraint(validationSpecification, element)
      case _       => // ignore
    }

    validationSpecification.replacesFunctionConstraint.foreach { functionName =>
      val nextSpecification = validationSpecification.copy(
          functionConstraint = Some(
              FunctionConstraint(message = Some(validationSpecification.message),
                                 internalFunction = Some(functionName)))
      )
      validateFunctionConstraint(nextSpecification, element)
    }

    validationSpecification.propertyConstraints.foreach { propertyConstraint =>
      PROPERTY_CONSTRAINT_VALIDATORS.foreach(
          _.validate(validationSpecification, propertyConstraint, element, reportBuilder))
    }

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

  private def reportFailure(validationSpecification: ValidationSpecification, id: String): Unit = {
    reportBuilder.reportFailure(validationSpecification, id, "")
  }

  private def reportFailure(validationSpecification: ValidationSpecification,
                            id: String,
                            propertyPath: Option[String] = None): Unit = {
    reportBuilder.reportFailure(validationSpecification, id, propertyPath.getOrElse(""))
  }
}
