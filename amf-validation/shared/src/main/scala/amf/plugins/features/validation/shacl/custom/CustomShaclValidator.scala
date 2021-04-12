package amf.plugins.features.validation.shacl.custom

import amf.core.model.document.{BaseUnit, FieldsFilter}
import amf.core.model.domain._
import amf.core.services.RuntimeValidator.CustomShaclFunctions
import amf.core.services.ValidationOptions
import amf.core.traversal.iterator.AmfElementStrategy
import amf.core.validation.core._
import amf.plugins.features.validation.shacl.custom.CustomShaclValidator.{
  PROPERTY_CONSTRAINT_VALIDATORS,
  computeConstraints
}
import amf.plugins.features.validation.shacl.custom.validators._

import scala.concurrent.{ExecutionContext, Future}

object CustomShaclValidator {

  private val PROPERTY_CONSTRAINT_VALIDATORS = Seq(
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

  protected def computeConstraints(customFunctions: CustomShaclFunctions): Set[ConstraintValidator] = Set(
      UnsupportedConstraint.closed,
      UnsupportedConstraint.custom,
      ShaclFunctionConstraint(customFunctions),
      ReplacedShaclFunctionConstraint(customFunctions),
      CompositePropertyConstraint(PROPERTY_CONSTRAINT_VALIDATORS)
  )
}

class CustomShaclValidator(model: BaseUnit, customFunctions: CustomShaclFunctions, options: ValidationOptions)(
    implicit executionContext: ExecutionContext) {

  private val reportBuilder: ReportBuilder = new ReportBuilder(options)
  private val constraints = Seq(
      CompositeMatchingClassOrInstanceConstraint(computeConstraints(customFunctions)),
      TargetObjectsOfConstraint
  )

  def run(validations: Set[ValidationSpecification]): Future[ValidationReport] = {
    model.iterator(AmfElementStrategy).foreach {
      case e: AmfObject => validateIdentityTransformation(validations, e)
      case _            =>
    }
    Future.successful(reportBuilder.build())
  }

  def applicable(
      validations: Set[ValidationSpecification]): (Set[ValidationSpecification], Set[ValidationSpecification]) = {
    validations.partition(v => constraints.exists(c => c.canValidate(v)))
  }

  private def validateIdentityTransformation(validations: Set[ValidationSpecification], element: AmfObject): Unit = {
    validations.foreach { specification =>
      constraints.foreach(_.validate(specification, element, reportBuilder))
    }
  }
}
