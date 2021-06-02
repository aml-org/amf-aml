package amf.plugins.features.validation.shacl

import amf.core.benchmark.ExecutionLog.log
import amf.core.model.document.BaseUnit
import amf.core.services.ShaclValidationOptions
import amf.core.validation.core.{SHACLValidator, ValidationReport, ValidationSpecification}
import amf.plugins.features.validation.AMFValidatorPlugin.customValidations
import amf.plugins.features.validation.emitters.{JSLibraryEmitter, ShaclJsonLdShapeGraphEmitter}

import scala.concurrent.{ExecutionContext, Future}

trait ShaclValidator {
  def validate(unit: BaseUnit, validations: Seq[ValidationSpecification])(
      implicit executionContext: ExecutionContext): Future[ValidationReport]
}

class FullShaclValidator(shacl: SHACLValidator, options: ShaclValidationOptions) extends ShaclValidator {

  def validate(model: BaseUnit, validations: Seq[ValidationSpecification])(
      implicit executionContext: ExecutionContext): Future[ValidationReport] = {

    log(s"AMFValidatorPlugin#shaclValidation: shacl validation for ${validations.size} validations")

    if (shacl.supportsJSFunctions) loadJSFunctions(validations)

    val shapes = customValidations(validations)

    log(s"AMFValidatorPlugin#shaclValidation: Invoking platform validation")

    val report = shacl.report(model, shapes, options)

    log(s"AMFValidatorPlugin#shaclValidation: validation finished")

    report
  }

  private def loadJSFunctions(validations: Seq[ValidationSpecification]) = {
    // TODO: Check the validation profile passed to JSLibraryEmitter, it contains the prefixes
    // for the functions
    val jsLibrary = new JSLibraryEmitter(None).emitJS(validations)

    jsLibrary match {
      case Some(code) =>
        shacl.registerLibrary(ShaclJsonLdShapeGraphEmitter.validationLibraryUrl, code)
      case _ => // ignore
    }

    log(s"AMFValidatorPlugin#shaclValidation: jsLibrary generated")
  }
}
