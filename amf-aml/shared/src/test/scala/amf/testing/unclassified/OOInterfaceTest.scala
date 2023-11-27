package amf.testing.unclassified

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.domain.DialectDomainElement
import amf.core.common.AsyncFunSuiteWithPlatformGlobalExecutionContext
import org.scalatest.matchers.should.Matchers

class OOInterfaceTest extends AsyncFunSuiteWithPlatformGlobalExecutionContext with Matchers {

  test("Test setLiteralProperty with List") {
    val dialect = "file://amf-aml/shared/src/test/resources/vocabularies2/dialects/set-literal-property/dialect.yaml"
    val dialectInstance =
      "file://amf-aml/shared/src/test/resources/vocabularies2/dialects/set-literal-property/instance.yaml"
    val propertyIri = "http://test.org#numbers"
    for {
      config        <- AMLConfiguration.predefined().withDialect(dialect)
      parsingResult <- config.baseUnitClient().parseDialectInstance(dialectInstance)
    } yield {
      val di      = parsingResult.dialectInstance
      val element = di.encodes.asInstanceOf[DialectDomainElement]
      element.withLiteralProperty(propertyIri, List(1, 2, 3))
      assert(element.getScalarByProperty(propertyIri).size == 3)
    }
  }

  test("Test setLiteralProperty with Int") {
    val dialect = "file://amf-aml/shared/src/test/resources/vocabularies2/dialects/set-literal-property/dialect.yaml"
    val dialectInstance =
      "file://amf-aml/shared/src/test/resources/vocabularies2/dialects/set-literal-property/instance.yaml"
    val propertyIri = "http://test.org#numbers"
    for {
      config        <- AMLConfiguration.predefined().withDialect(dialect)
      parsingResult <- config.baseUnitClient().parseDialectInstance(dialectInstance)
    } yield {
      val di      = parsingResult.dialectInstance
      val element = di.encodes.asInstanceOf[DialectDomainElement]
      element.withLiteralProperty(propertyIri, 1)
      assert(element.getScalarByProperty(propertyIri).size == 1)
    }
  }

}
