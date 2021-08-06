package amf.testing.config

import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping}
import amf.aml.client.scala.AMLConfiguration
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.aml.internal.render.plugin.AMLDialectInstanceRenderingPlugin
import org.scalatest.{Assertion, AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class AMLConfigurationPluginTest extends AsyncFunSuite with Matchers {

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath      = "file://amf-aml/shared/src/test/resources/vocabularies2/config/"
  val firstDialect  = "dialect_0.yaml"
  val secondDialect = "dialect_1.yaml"

  test("Loading dialects with same name and version should update plugins") {
    withLoadedConfig(pathName = "dialect_0.yaml") { config =>
      withLoadedConfig(config, pathName = "dialect_1.yaml") { finalConfig =>
        assertDialectAndPluginAmount(expectedAmount = 1, finalConfig)

        val dialect = finalConfig.configurationState().getDialects().head
        dialect.declares.head.asInstanceOf[NodeMapping].propertiesMapping() should have length 2
      }
    }
  }

  private def assertDialectAndPluginAmount(expectedAmount: Int, config: AMLConfiguration) = {
    val dialects = config.configurationState().getDialects()
    dialects.size shouldBe 1
    config.registry.plugins.parsePlugins
      .filter(_.isInstanceOf[AMLDialectInstanceParsingPlugin]) should have length 1
    config.registry.plugins.renderPlugins
      .filter(_.isInstanceOf[AMLDialectInstanceRenderingPlugin]) should have length 1
  }

  private def withLoadedConfig(config: AMLConfiguration = AMLConfiguration.predefined(), pathName: String)(
      block: AMLConfiguration => Future[Assertion]) = {
    val futureResult = config.baseUnitClient().parseDialect(basePath + pathName)
    futureResult.flatMap { result =>
      result.conforms shouldBe true
      block(config.withDialect(result.dialect))
    }
  }
}
