package amf.plugins.features

import amf.core.registries.AMFPluginsRegistry
import amf.plugins.document.graph.AMFGraphParsePlugin
import amf.plugins.document.vocabularies.AMLParsePlugin
import amf.plugins.features.validation.custom.AMFValidatorPlugin

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
object AMFCustomValidation {
  def register(): Unit = {
    amf.Core.registerPlugin(AMFValidatorPlugin)
  }
}
