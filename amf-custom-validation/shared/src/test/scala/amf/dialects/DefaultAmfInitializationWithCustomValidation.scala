package amf.dialects

import amf.plugins.document.graph.AMFGraphPlugin
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.features.validation.custom.AMFValidatorPlugin
import amf.plugins.syntax.SYamlSyntaxPlugin
import org.scalatest.{AsyncFunSuite, BeforeAndAfterAll}

import scala.concurrent.Future

trait DefaultAmfInitializationWithCustomValidation extends AsyncFunSuite with BeforeAndAfterAll {
  private def init(): Future[Unit] = {
    amf.core.AMF.init().map { _ =>
      amf.core.registries.AMFPluginsRegistry.registerSyntaxPlugin(SYamlSyntaxPlugin)
      amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(AMFGraphPlugin)
      amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(AMLPlugin)
      amf.core.AMF.registerPlugin(AMFValidatorPlugin)
      AMFValidatorPlugin.init()
    }
  }

  override protected def beforeAll(): Unit = init()
}
