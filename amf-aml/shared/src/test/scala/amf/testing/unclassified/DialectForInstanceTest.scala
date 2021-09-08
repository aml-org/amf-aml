package amf.testing.unclassified

import amf.aml.client.scala.AMLConfiguration
import org.scalatest.{Assertion, AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class DialectForInstanceTest extends AsyncFunSuite with Matchers {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  private def basePath: String    = "amf-aml/shared/src/test/resources/vocabularies2/instances/nested-dialects/"
  private val baseDialect: String = buildPath("base-dialect.yaml")

  test("Dialect instance without nested dialect") {
    val instancePath: String = buildPath("simple-instance.yaml")
    run(instancePath, 1, 1, Seq("Test 1.0"))
  }

  test("Dialect instance with 1 nested dialect") {
    val instancePath: String = buildPath("single-nested-instance.yaml")
    run(instancePath, 1, 2, Seq("Test 1.0", "FirstNested 1.0"))
  }

  test("Dialect instance with 2 nested dialect") {
    val instancePath: String = buildPath("double-nested-instance.yaml")
    run(instancePath, 1, 3, Seq("Test 1.0", "FirstNested 1.0", "SecondNested 1.0"))
  }

  // Uncomment when collection without plugin is developed
  ignore("Dialect instance with inline dialect reference") {
    val instancePath: String = buildPath("inlined-dialect-ref-instance.yaml")
    run(instancePath, 0, 1, Seq("Test 1.0"), None)
  }

  private def run(instancePath: String,
                  baseCount: Int,
                  expectedCount: Int,
                  expectedNames: Seq[String],
                  initialDialect: Option[String] = Some(baseDialect)): Future[Assertion] = {
    val initialConfig = initialDialect match {
      case Some(url) => AMLConfiguration.predefined().withDialect(url)
      case None      => Future(AMLConfiguration.predefined())
    }
    initialConfig.flatMap { baseConfig =>
      baseConfig.forInstance(instancePath).map { newConfig =>
        val baseDialect = baseConfig.configurationState().getDialects()
        val newDialects = newConfig.configurationState().getDialects()
        expectedNames.foreach(n => assert(newDialects.exists(d => d.nameAndVersion() == n)))
        assert(baseDialect.size == baseCount)
        assert(newDialects.size == expectedCount)
      }
    }
  }

  private def buildPath(path: String): String = s"file://$basePath$path"

}
