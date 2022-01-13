package amf.testing.semantic

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.internal.remote.Syntax.Yaml
import amf.testing.common.cycling.FunSuiteCycleTests

import scala.concurrent.Future

class CycledSemanticRenderTest extends FunSuiteCycleTests {
  override def basePath: String = "amf-aml/shared/src/test/resources/vocabularies2/semantic/"

  test("Render object extension") {
    getConfig("dialect-extensions.yaml").flatMap { config =>
      cycle("instance.yaml", "instance.cycled.yaml", Some(Yaml), amlConfig = config)
    }
  }

  test("Render scalar extension") {
    getConfig("dialect-scalar-extensions.yaml").flatMap { config =>
      cycle("instance-scalar.yaml", "instance-scalar.yaml", Some(Yaml), amlConfig = config)
    }
  }

  private def getConfig(dialect: String): Future[AMLConfiguration] = {
    AMLConfiguration
      .predefined()
      .withRenderOptions(RenderOptions().withPrettyPrint.withCompactUris)
      .withErrorHandlerProvider(() => UnhandledErrorHandler)
      .withDialect(s"file://$basePath" + dialect)
  }
}
