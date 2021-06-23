package amf.validation.internal.shacl

import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.validation.core.{
  SHACLValidator,
  ShaclValidationOptions,
  ValidationReport,
  ValidationSpecification
}
import amf.validation.internal.emitters.{JSLibraryEmitter, ShaclJsonLdShapeGraphEmitter}

import scala.concurrent.{ExecutionContext, Future}

trait ShaclValidator {
  def validate(unit: BaseUnit, validations: Seq[ValidationSpecification])(
      implicit executionContext: ExecutionContext): Future[ValidationReport]
}

class FullShaclValidator(shacl: SHACLValidator, options: ShaclValidationOptions) extends ShaclValidator {

  def validate(model: BaseUnit, validations: Seq[ValidationSpecification])(
      implicit executionContext: ExecutionContext): Future[ValidationReport] = {

    if (shacl.supportsJSFunctions) loadJSFunctions(validations)

    val shapes = validations.filter(s => !s.isParserSide)

    val report = shacl.report(model, shapes, options)

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
  }
}
