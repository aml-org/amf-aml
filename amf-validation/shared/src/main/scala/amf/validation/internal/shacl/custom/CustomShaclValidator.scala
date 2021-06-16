package amf.validation.internal.shacl.custom

import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.traversal.iterator.AmfElementStrategy
import amf.core.internal.metamodel.Field
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.validation.core.{ShaclValidationOptions, ValidationReport, ValidationSpecification}
import amf.validation.internal.shacl.ShaclValidator
import amf.validation.internal.shacl.custom.CustomShaclValidator.{
  CustomShaclFunctions,
  PROPERTY_CONSTRAINT_VALIDATORS,
  computeConstraints
}
import amf.validation.internal.shacl.custom.validators._

import scala.concurrent.{ExecutionContext, Future}

object CustomShaclValidator {

  type PropertyInfo = (Annotations, Field)
  // When no property info is provided violation is thrown in domain element level
  type CustomShaclFunction  = (AmfObject, Option[PropertyInfo] => Unit) => Unit
  type CustomShaclFunctions = Map[String, CustomShaclFunction]

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

class CustomShaclValidator(customFunctions: CustomShaclFunctions, options: ShaclValidationOptions)
    extends ShaclValidator {

  private val reportBuilder: ReportBuilder = new ReportBuilder(options)
  private val constraints = Seq(
      CompositeMatchingClassOrInstanceConstraint(computeConstraints(customFunctions)),
      TargetObjectsOfConstraint
  )

  def validate(model: BaseUnit, validations: Seq[ValidationSpecification])(
      implicit executionContext: ExecutionContext): Future[ValidationReport] = {
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

  private def validateIdentityTransformation(validations: Seq[ValidationSpecification], element: AmfObject): Unit = {
    validations.foreach { specification =>
      constraints.foreach(_.validate(specification, element, reportBuilder))
    }
  }
}
