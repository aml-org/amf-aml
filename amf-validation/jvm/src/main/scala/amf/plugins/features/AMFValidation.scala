package amf.plugins.features

import amf.plugins.features.validation.AMFValidatorPlugin

object AMFValidation {
  def register(): Unit = {
    amf.Core.registerPlugin(AMFValidatorPlugin)
  }
}
