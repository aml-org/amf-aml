package amf.testing.parsing

import amf.aml.client.scala.model.document.DialectInstance
import amf.aml.client.scala.{AMLConfiguration, AMLDialectInstanceResult}
import amf.core.client.scala.model.domain.AmfArray
import org.mulesoft.common.client.lexical.PositionRange
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class DialectInstanceAssertionTest extends AsyncFunSuite with Matchers {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  val basePath = "file://amf-aml/shared/src/test/resources/vocabularies2/instances/"

  def parseDialectInstance(dialectPath: String, instancePath: String): Future[AMLDialectInstanceResult] = {
    AMLConfiguration.predefined().withDialect(dialectPath) flatMap { config =>
      config.baseUnitClient().parseDialectInstance(instancePath)
    }
  }

  def getDialectInstanceFirstValue(di: DialectInstance) =
    di.encodes.fields
      .fields()
      .head
      .value
      .value
      .asInstanceOf[AmfArray]
      .values
      .head

  // APIMF-3604 - W-10547579
  test("MapTermKey should be included in a Dialect Instance lexical info") {
    val dialect  = s"$basePath/rest-connector/dialect.yaml"
    val instance = s"$basePath/rest-connector/instance.yaml"
    parseDialectInstance(dialect, instance) flatMap { result =>
      val lexical = getDialectInstanceFirstValue(result.dialectInstance).annotations.lexical()
      lexical shouldEqual PositionRange((3, 2), (6, 0))
    }
  }

  // APIMF-3604 - W-10547579
  test("MapTermKey should be included in a Dialect Instance lexical info with a dialect with a union") {
    val dialectUnion = s"$basePath/rest-connector/dialect-union.yaml"
    val instance     = s"$basePath/rest-connector/instance.yaml"
    parseDialectInstance(dialectUnion, instance) flatMap { result =>
      val lexical = getDialectInstanceFirstValue(result.dialectInstance).annotations.lexical()
      lexical shouldEqual PositionRange((3, 2), (6, 0))
    }
  }

}
