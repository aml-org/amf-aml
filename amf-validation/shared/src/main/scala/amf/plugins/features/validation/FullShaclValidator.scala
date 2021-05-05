package amf.plugins.features.validation

import amf.core.benchmark.ExecutionLog
import amf.core.benchmark.ExecutionLog.log
import amf.core.model.document.BaseUnit
import amf.core.services.ValidationOptions
import amf.core.validation.EffectiveValidations
import amf.core.validation.core.ValidationReport
import amf.plugins.features.validation.AMFValidatorPlugin.customValidations
import amf.plugins.features.validation.emitters.{JSLibraryEmitter, ShaclJsonLdShapeGraphEmitter}

import scala.concurrent.{ExecutionContext, Future}

class FullShaclValidator {

  def validate(model: BaseUnit, validations: EffectiveValidations, options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationReport] = {

    log(s"AMFValidatorPlugin#shaclValidation: shacl validation for ${validations.effective.values.size} validations")

    if (PlatformValidator.instance.supportsJSFunctions) loadJSFunctions(validations)

    val shapes = customValidations(validations)

    log(s"AMFValidatorPlugin#shaclValidation: Invoking platform validation")

    val report = PlatformValidator.instance.report(model, shapes, options)

    log(s"AMFValidatorPlugin#shaclValidation: validation finished")

    report
  }

  private def loadJSFunctions(validations: EffectiveValidations) = {
    // TODO: Check the validation profile passed to JSLibraryEmitter, it contains the prefixes
    // for the functions
    val jsLibrary = new JSLibraryEmitter(None).emitJS(validations.effective.values.toSeq)

    jsLibrary match {
      case Some(code) =>
        PlatformValidator.instance.registerLibrary(ShaclJsonLdShapeGraphEmitter.validationLibraryUrl, code)
      case _ => // ignore
    }

    log(s"AMFValidatorPlugin#shaclValidation: jsLibrary generated")
  }
}
