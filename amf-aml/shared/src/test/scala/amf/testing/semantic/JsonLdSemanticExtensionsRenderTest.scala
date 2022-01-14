package amf.testing.semantic

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.Syntax.JsonLd
import amf.testing.common.cycling.FunSuiteCycleTests
import org.mulesoft.common.test.AsyncBeforeAndAfterEach

import scala.concurrent.Future

class JsonLdSemanticExtensionsRenderTest extends FunSuiteCycleTests with AsyncBeforeAndAfterEach {

  override def basePath: String = "amf-aml/shared/src/test/resources/vocabularies2/semantic/"

  test("Render flattened semantic extensions to JSON-LD") {
    getConfig("dialect-extensions.yaml").flatMap { config =>
      cycle("instance.yaml", golden = "instance.jsonld", syntax = Some(JsonLd), amlConfig = config)
    }
  }

  test("Render flattened semantic extensions with scalar range to JSON-LD") {
    getConfig("dialect-scalar-extensions.yaml").flatMap { config =>
      cycle("instance-scalar.yaml", golden = "instance-scalar.jsonld", syntax = Some(JsonLd), amlConfig = config)
    }
  }

  /** Method for transforming parsed unit. Override if necessary. */
  override def transform(unit: BaseUnit, config: CycleConfig, amlConfig: AMLConfiguration): BaseUnit = {
    amlConfig.baseUnitClient().transform(unit).baseUnit
  }

  private def getConfig(dialect: String): Future[AMLConfiguration] = {
    AMLConfiguration
      .predefined()
      .withRenderOptions(RenderOptions().withPrettyPrint.withCompactUris)
      .withErrorHandlerProvider(() => UnhandledErrorHandler)
      .withDialect(s"file://$basePath" + dialect)
  }
}
