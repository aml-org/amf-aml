package amf.testing.unclassified

import amf.aml.client.scala.AMLConfiguration
import amf.core.common.AsyncFunSuiteWithPlatformGlobalExecutionContext
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class SemanticExtensionParserTest extends AsyncFunSuiteWithPlatformGlobalExecutionContext with Matchers {

  private def basePath: String = "file://amf-aml/shared/src/test/resources/vocabularies2/dialects"

  test("Parse SE 1") {
    run(s"$basePath/annotation-mappings-finding/dialect.yaml", Seq("maintainer", "contributor"))
  }

  test("Parse SE 2") {
    run(
        s"$basePath/annotation-mappings-with-extra-facets/dialect.yaml",
        Seq("maintainer", "rateLimiting", "owner", "anypointId", "accountType", "contactEmail", "ldapReferences")
    )
  }

  private def run(path: String, se: Seq[String]): Future[Assertion] =
    AMLConfiguration.predefined().withDialect(path) map { config =>
      val dialects = config.configurationState().getDialects()
      assert(dialects.size == 1)
      val registeredDialect = dialects.head
      val extensions        = registeredDialect.extensions()
      assert(extensions.nonEmpty)
      assert(extensions.size == se.size)
      assert(extensions.forall(e => se.contains(e.extensionName().value())))
    }
}
