package amf.testing.semantic

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.document.DialectInstance
import amf.aml.client.scala.model.domain.DialectDomainElement
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.common.AsyncFunSuiteWithPlatformGlobalExecutionContext
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.parser.domain.Value
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class AppliedExtensionTest extends AsyncFunSuiteWithPlatformGlobalExecutionContext with Matchers {

  val basePath = "file://amf-aml/shared/src/test/resources/vocabularies2/semantic/"

  test("Applied extensions") {
    assertModel("dialect-extensions.yaml", "instance.yaml") { instance =>
      val extension = instance.encodes.customDomainProperties.head

      extension.name.value() shouldBe "maintainer"
      assertAnnotations(extension.fields.getValueAsOption("http://a.ml/vocab#maintainer").get)

      val maintainerInstance = extension.graph.getObjectByProperty("http://a.ml/vocab#maintainer").head
      val users              = maintainerInstance.graph.getObjectByProperty("http://a.ml/vocabularies/data#users")
      users.foreach { user =>
        user.graph.containsProperty("http://a.ml/vocab#username") shouldBe true
        user.graph.containsProperty("http://a.ml/vocab#contributor") shouldBe true
      }
      users should have length 2
    }
  }

  test("Applied and resolved extensions") {
    assertResolvedModel("dialect-extensions.yaml", "instance.yaml") { instance =>
      val maintainer = instance.encodes.graph.getObjectByProperty("http://a.ml/vocab#maintainer").head

      val users = maintainer.graph.getObjectByProperty("http://a.ml/vocabularies/data#users")
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
      val extension = instance.encodes.customDomainProperties.head

      extension.graph.containsProperty(PREFERRED_PROPERTY_TERM) shouldBe false

      val org =
        instance.encodes.asInstanceOf[DialectDomainElement].graph.getObjectByProperty("http://a.ml/vocab#org").head
      org.customDomainProperties.head.graph.containsProperty(PREFERRED_PROPERTY_TERM) shouldBe true
    }
  }

  test("Applied scalar extensions") {
    assertModel("dialect-scalar-extensions.yaml", "instance-scalar.yaml") { instance =>
      val extension = instance.encodes.customDomainProperties.head
      extension.name.value() shouldBe "maintainer"
      assertAnnotations(extension.fields.getValueAsOption("http://a.ml/vocab#maintainer").get)

      val maintainerInstance = extension.graph.scalarByProperty("http://a.ml/vocab#maintainer").head
      maintainerInstance shouldEqual "Some value"
    }
  }

  test("Nested semex 1") {
    assertModel("dialect-scalar-extensions.yaml", "instance-scalar.yaml") { instance =>
      val extension = instance.encodes.customDomainProperties.head
      extension.name.value() shouldBe "maintainer"
      assertAnnotations(extension.fields.getValueAsOption("http://a.ml/vocab#maintainer").get)

      val maintainerInstance = extension.graph.scalarByProperty("http://a.ml/vocab#maintainer").head
      maintainerInstance shouldEqual "Some value"
    }
  }

  private def assertAnnotations(value: Value): Unit = {
    value.annotations.nonEmpty shouldBe true
    value.annotations.find(classOf[LexicalInformation]) shouldNot be(empty)
  }

  def assertModel(dialect: String, instance: String, shouldTransform: Boolean = false)(
      assertion: DialectInstance => Assertion
  ): Future[Assertion] = {
    val config = AMLConfiguration
      .predefined()
      .withErrorHandlerProvider(() => UnhandledErrorHandler)
      .withDialect(basePath + dialect)
    for {
      nextConfig <- config
      instance   <- nextConfig.baseUnitClient().parseDialectInstance(basePath + instance)
    } yield {
      val finalModel =
        if (shouldTransform) transform(nextConfig, instance.dialectInstance) else instance.dialectInstance
      assertion(finalModel)
    }
  }

  private def transform(config: AMLConfiguration, instance: DialectInstance) =
    config.baseUnitClient().transform(instance).baseUnit.asInstanceOf[DialectInstance]
  def assertResolvedModel(dialect: String, instance: String)(
      assertion: DialectInstance => Assertion
  ): Future[Assertion] = {
    assertModel(dialect, instance, shouldTransform = true)(assertion)
  }
}
