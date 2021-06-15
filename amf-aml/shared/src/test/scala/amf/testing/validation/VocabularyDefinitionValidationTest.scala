package amf.testing.validation

import amf.client.environment.AMLConfiguration
import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.core.internal.parser.ParseConfiguration
import amf.core.internal.validation.ValidationConfiguration
import amf.core.io.FileAssertionTest
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.client.scala.validation.AMFValidationReport
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}
import amf.validation.internal.emitters.ValidationReportJSONLDEmitter
import org.scalatest
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class VocabularyDefinitionValidationTest
    extends AsyncFunSuite
    with Matchers
    with FileAssertionTest
    with PlatformSecrets {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/validations/vocabularies"

  test("Test repeated term in property terms and class terms") {
    validate("vocabulary.yaml", Some("validation.jsonld"), "repeated-term")
  }

  test("Test missing class term") {
    validate("vocabulary.yaml", Some("validation.jsonld"), "missing-class-term")
  }

  test("Test missing property term") {
    validate("vocabulary.yaml", Some("validation.jsonld"), "missing-property-term")
  }

  private def compilerContext(url: String, amfConfig: AMLConfiguration) =
    new CompilerContextBuilder(url, platform, amfConfig.parseConfiguration).build()

  protected def validate(vocabulary: String,
                         goldenReport: Option[String] = None,
                         path: String): Future[scalatest.Assertion] = {
    val eh            = DefaultErrorHandler()
    val configuration = AMLConfiguration.forEH(eh)
    val report = for {
      report <- configuration
        .createClient()
        .parseVocabulary(s"file://$basePath/$path/$vocabulary")
        .map(AMFValidationReport.unknownProfile(_))
    } yield {
      report
    }

    report.flatMap { re =>
      goldenReport match {
        case Some(golden) =>
          writeTemporaryFile(s"$basePath/$path/$golden")(ValidationReportJSONLDEmitter.emitJSON(re))
            .flatMap(assertDifferences(_, s"$basePath/$path/$golden"))
        case None => re.conforms should be(true)
      }
    }
  }

}
