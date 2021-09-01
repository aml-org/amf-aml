package amf.rdf.internal.converter

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.validation.CoreValidations.UnableToConvertToScalar

trait Converter {

  protected def conversionValidation(message: String)(implicit errorHandler: AMFErrorHandler) = {
    errorHandler.violation(UnableToConvertToScalar, "", message, "")
    None
  }
}
