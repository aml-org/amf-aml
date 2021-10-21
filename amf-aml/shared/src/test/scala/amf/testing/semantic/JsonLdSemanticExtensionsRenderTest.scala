package amf.testing.semantic

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.Syntax.JsonLd
import amf.testing.common.cycling.FunSuiteCycleTests
import org.scalatest.{AsyncFunSuite, Matchers}

class JsonLdSemanticExtensionsRenderTest extends FunSuiteCycleTests {
  override def basePath: String = "amf-aml/shared/src/test/resources/vocabularies2/semantic/"

  test("Render flattened semantic extensions to JSON-LD") {
    cycle("instance.yaml",
          golden = "instance.jsonld",
          syntax = Some(JsonLd),
          amlConfig = getConfig("dialect-extensions.yaml"))
  }

  /** Method for transforming parsed unit. Override if necessary. */
  override def transform(unit: BaseUnit, config: CycleConfig, amlConfig: AMLConfiguration): BaseUnit = {
    amlConfig.baseUnitClient().transform(unit).baseUnit
  }

  private def getConfig(dialect: String): AMLConfiguration = {
    val config = AMLConfiguration
      .predefined()
      .withRenderOptions(RenderOptions().withPrettyPrint.withCompactUris)
      .withErrorHandlerProvider(() => UnhandledErrorHandler)
      .withDialect(s"file://$basePath" + dialect)

    await { config }
  }
}
