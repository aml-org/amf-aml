package amf.testing.jsonldcycle

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.document.DialectInstance
import amf.core.client.scala.model.document.Document
import amf.core.internal.remote.Mimes
import amf.core.io.FileAssertionTest
import amf.testing.common.utils.DialectValidation
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class ToAndFromJsonLDInstanceTest extends AsyncFunSuite with FileAssertionTest with Matchers {

  private val basePath = "amf-aml/shared/src/test/resources/vocabularies2/jsonldcycle/"

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Test cycle for any-seq(ValidationProfile)") {
    val current = basePath + "any-seq/"
    for {
      c              <- AMLConfiguration.predefined().withDialect("file://" + current + "dialect.yaml")
      instanceResult <- c.baseUnitClient().parse("file://" + current + "instance.yaml").map(_.baseUnit)
      jsonLD = c.baseUnitClient().render(instanceResult, Mimes.`application/ld+json`)
      actual       <- writeTemporaryFile(current + "instance.yaml.jsonld")(jsonLD)
      assertion    <- assertDifferences(actual, current + "instance.yaml.jsonld")
      jsonLDParsed <- c.baseUnitClient().parse("file://" + current + "instance.yaml.jsonld")
    } yield {
      assert(assertion == succeed)
      jsonLDParsed.baseUnit
        .asInstanceOf[DialectInstance]
        .encodes
        .graph
        .scalarByProperty("https://schema.org/in")
        .size shouldBe 2
    }
  }
}
