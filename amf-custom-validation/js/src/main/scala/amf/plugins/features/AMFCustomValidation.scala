package amf.plugins.features

import amf.plugins.features.validation.custom.AMFValidatorPlugin

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
object AMFCustomValidation {
  def register(): Unit = {
    amf.Core.registerPlugin(AMFValidatorPlugin)
  }
}
