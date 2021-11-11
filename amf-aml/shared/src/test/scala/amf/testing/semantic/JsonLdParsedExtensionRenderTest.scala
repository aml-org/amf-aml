package amf.testing.semantic

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.internal.remote.Syntax.JsonLd
import amf.testing.common.cycling.FunSuiteCycleTests
import org.mulesoft.common.test.AsyncBeforeAndAfterEach

import scala.concurrent.Future

class JsonLdParsedExtensionRenderTest extends FunSuiteCycleTests with AsyncBeforeAndAfterEach {

  override def basePath: String = "amf-aml/shared/src/test/resources/vocabularies2/semantic/"

  test("Non-flattened semantic extensions should not be rendered to JSON-LD") {
    getConfig("dialect-extensions.yaml").flatMap { config =>
      cycle("instance.yaml", golden = "instance.parsed.jsonld", syntax = Some(JsonLd), amlConfig = config)
    }
  }

  test("Non-flattened semantic extensions with scalar range should not be rendered to JSON-LD") {
    getConfig("dialect-scalar-extensions.yaml").flatMap { config =>
      cycle("instance-scalar.yaml",
            golden = "instance-scalar.parsed.jsonld",
            syntax = Some(JsonLd),
            amlConfig = config)
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
