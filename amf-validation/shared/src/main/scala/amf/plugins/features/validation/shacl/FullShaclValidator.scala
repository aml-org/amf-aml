package amf.plugins.features.validation.shacl

import amf.core.benchmark.ExecutionLog.log
import amf.core.model.document.BaseUnit
import amf.core.services.ValidationOptions
import amf.core.validation.EffectiveValidations
import amf.core.validation.core.{ValidationReport, ValidationSpecification}
import amf.plugins.features.validation.AMFValidatorPlugin.customValidations
import amf.plugins.features.validation.emitters.{JSLibraryEmitter, ShaclJsonLdShapeGraphEmitter}
import amf.plugins.features.validation.PlatformValidator

import scala.concurrent.{ExecutionContext, Future}

class FullShaclValidator {

  def validate(model: BaseUnit, validations: Seq[ValidationSpecification], options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationReport] = {

    log(s"AMFValidatorPlugin#shaclValidation: shacl validation for ${validations.size} validations")

    if (PlatformValidator.instance.supportsJSFunctions) loadJSFunctions(validations)

    val shapes = customValidations(validations)

    log(s"AMFValidatorPlugin#shaclValidation: Invoking platform validation")

    val report = PlatformValidator.instance.report(model, shapes, options)

    log(s"AMFValidatorPlugin#shaclValidation: validation finished")

    report
  }

  private def loadJSFunctions(validations: Seq[ValidationSpecification]) = {
    // TODO: Check the validation profile passed to JSLibraryEmitter, it contains the prefixes
    // for the functions
    val jsLibrary = new JSLibraryEmitter(None).emitJS(validations)

    jsLibrary match {
      case Some(code) =>
        PlatformValidator.instance.registerLibrary(ShaclJsonLdShapeGraphEmitter.validationLibraryUrl, code)
      case _ => // ignore
    }

    log(s"AMFValidatorPlugin#shaclValidation: jsLibrary generated")
  }
}
