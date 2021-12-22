package amf.testing.config

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.domain.NodeMapping
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.aml.internal.render.plugin.AMLDialectInstanceRenderingPlugin
import amf.core.internal.resource.AMFResolvers.platform.fs
import org.scalatest.{Assertion, AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class AMLConfigurationPluginTest extends AsyncFunSuite with Matchers {

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  val basePath                                             = "amf-aml/shared/src/test/resources/vocabularies2/config/"
  val movieDialect0                                        = s"$basePath/movie_dialect_0.yaml"
  val movieDialect1                                        = s"$basePath/movie_dialect_1.yaml"
  val personDialect0                                       = s"$basePath/person_dialect_0.yaml"

  test("Loading dialects with same name and version should update plugins") {
    withLoadedConfig(path = s"file://$movieDialect0") { config =>
      withLoadedConfig(config, path = s"file://$movieDialect1") { finalConfig =>
        assertDialectAndPluginAmount(expectedAmount = 1, finalConfig)

        val dialect = finalConfig.configurationState().getDialects().head
        dialect.declares.head.asInstanceOf[NodeMapping].propertiesMapping() should have length 2
      }
    }
  }

  test("Parsing dialects from same content should produce same ID") {
    val client = AMLConfiguration.predefined().baseUnitClient()
    for {
      contentA <- fs.asyncFile(movieDialect0).read()
      contentB <- fs.asyncFile(movieDialect1).read()
      parsedA  <- client.parseContent(contentA.toString)
      parsedB  <- client.parseContent(contentB.toString)
    } yield {
      assert(parsedA.baseUnit.id == parsedB.baseUnit.id)
    }
  }

  test("Parsing dialects from different contents should produce different IDs") {
    val client = AMLConfiguration.predefined().baseUnitClient()

    for {
      contentA <- fs.asyncFile(movieDialect0).read()
      contentB <- fs.asyncFile(personDialect0).read()
      parsedA  <- client.parseContent(contentA.toString)
      parsedB  <- client.parseContent(contentB.toString)
    } yield {
      assert(parsedA.baseUnit.id != parsedB.baseUnit.id)
    }
  }

  private def assertDialectAndPluginAmount(expectedAmount: Int, config: AMLConfiguration) = {
    val dialects = config.configurationState().getDialects()
    dialects.size shouldBe 1
    config.registry.getPluginsRegistry.rootParsePlugins
      .filter(_.isInstanceOf[AMLDialectInstanceParsingPlugin]) should have length 1
    config.registry.getPluginsRegistry.renderPlugins
      .filter(_.isInstanceOf[AMLDialectInstanceRenderingPlugin]) should have length 1
  }

  private def withLoadedConfig(config: AMLConfiguration = AMLConfiguration.predefined(), path: String)(
      block: AMLConfiguration => Future[Assertion]) = {
    val futureResult = config.baseUnitClient().parseDialect(path)
    futureResult.flatMap { result =>
      result.conforms shouldBe true
      block(config.withDialect(result.dialect))
    }
  }
}
