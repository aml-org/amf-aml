package amf.testing.semantic

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.document.DialectInstance
import amf.aml.client.scala.model.domain.DialectDomainElement
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import org.scalatest.{Assertion, AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class SemanticTransformationStageTest extends AsyncFunSuite with Matchers {

  val basePath                           = "file://amf-aml/shared/src/test/resources/vocabularies2/semantic/"
  override implicit val executionContext = ExecutionContext.Implicits.global

  test("Applied extensions") {
    assertModel("dialect-extensions.yaml", "instance.yaml") { instance =>
      val extendedElement    = instance.encodes
      val maintainerInstance = extendedElement.graph.getObjectByProperty("http://a.ml/vocab#maintainer").head
      val users              = maintainerInstance.graph.getObjectByProperty("http://a.ml/vocabularies/data#users")
      users.foreach { user =>
        user.graph.containsProperty("http://a.ml/vocab#username") shouldBe true
        user.graph.containsProperty("http://a.ml/vocab#contributor") shouldBe true
      }
      users should have length 2
    }
  }

  test("Applied extensions with wrong target") {

    val PREFERRED_PROPERTY_TERM = "http://a.ml/vocab#preference"
    assertModel("wrong-target-extensions.yaml", "instance-wrong-target.yaml") { instance =>
      val extension = instance.encodes

      extension.graph.containsProperty(PREFERRED_PROPERTY_TERM) shouldBe false

      val org =
        instance.encodes.asInstanceOf[DialectDomainElement].graph.getObjectByProperty("http://a.ml/vocab#org").head
      org.graph.containsProperty(PREFERRED_PROPERTY_TERM) shouldBe true
    }
  }

  def assertModel(dialect: String, instance: String)(assertion: DialectInstance => Assertion): Future[Assertion] = {
    val config = AMLConfiguration
      .predefined()
      .withErrorHandlerProvider(() => UnhandledErrorHandler)
      .withDialect(basePath + dialect)
    for {
      nextConfig <- config
      instance   <- nextConfig.baseUnitClient().parseDialectInstance(basePath + instance)
    } yield {
      val transformResult = nextConfig.baseUnitClient().transform(instance.dialectInstance)
      assertion(transformResult.baseUnit.asInstanceOf[DialectInstance])
    }
  }
}
