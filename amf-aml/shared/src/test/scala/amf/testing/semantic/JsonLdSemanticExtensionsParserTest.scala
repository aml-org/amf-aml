package amf.testing.semantic

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.document.DialectInstance
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class JsonLdSemanticExtensionsParserTest extends AsyncFunSuite with Matchers {

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  def basePath: String = "file://amf-aml/shared/src/test/resources/vocabularies2/semantic/"

  test("Parse JSON-LD with semantic extensions in it") {
    getConfig("dialect-extensions.yaml").flatMap { config =>
      config.baseUnitClient().parse(s"${basePath}instance.jsonld").map { result =>
        val extendedElement = result.baseUnit.asInstanceOf[DialectInstance].encodes
        val maintainerNode  = extendedElement.graph.getObjectByProperty("http://a.ml/vocab#maintainer")
        maintainerNode shouldNot be(empty)
        val maintainerInstance = maintainerNode.head
        val users              = maintainerInstance.graph.getObjectByProperty("http://a.ml/vocabularies/data#users")
        users.foreach { user =>
          user.graph.containsProperty("http://a.ml/vocab#username") shouldBe true
          user.graph.containsProperty("http://a.ml/vocab#contributor") shouldBe true
        }
        users should have length 2
      }
    }
  }

  test("Parse JSON-LD with scalar semantic extensions in it") {
    getConfig("dialect-scalar-extensions.yaml").flatMap { config =>
      config.baseUnitClient().parse(s"${basePath}instance-scalar.jsonld").map { result =>
        val extendedElement = result.baseUnit.asInstanceOf[DialectInstance].encodes
        val maintainerNode  = extendedElement.graph.scalarByProperty("http://a.ml/vocab#maintainer").head
        maintainerNode shouldEqual "Some value"
      }
    }
  }

  private def getConfig(dialect: String): Future[AMLConfiguration] = {
    AMLConfiguration
      .predefined()
      .withRenderOptions(RenderOptions().withPrettyPrint.withCompactUris)
      .withErrorHandlerProvider(() => UnhandledErrorHandler)
      .withDialect(basePath + dialect)
  }
}
