package amf.plugins.features

import amf.plugins.features.validation.custom.AMFValidatorPlugin

object AMFCustomValidation {
  def register(): Unit = {
    amf.Core.registerPlugin(AMFValidatorPlugin)
  }
}
