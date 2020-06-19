package amf.client.render

import amf.client.environment.Environment
import amf.core.registries.AMFPluginsRegistry
import amf.plugins.document.vocabularies.plugin.AMLPlugin

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
class Aml10Renderer private (override val mediaType: String, private val env: Option[Environment])
    extends Renderer("AML 1.0", mediaType, env) {

  @JSExportTopLevel("Aml10Renderer")
  def this() = this("application/yaml", None)
  @JSExportTopLevel("Aml10Renderer")
  def this(mediaType: String) = this(mediaType, None)
  def this(env: Environment) = this("application/yaml", Some(env))
  def this(mediaType: String, env: Environment) = this("application/yaml", Some(env))

  AMFPluginsRegistry.registerDocumentPlugin(AMLPlugin)
}
