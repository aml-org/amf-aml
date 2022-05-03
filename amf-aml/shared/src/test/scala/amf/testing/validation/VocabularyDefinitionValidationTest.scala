package amf.testing.validation

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.validation.AMFValidationReport
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.io.FileAssertionTest
import amf.validation.internal.emitters.ValidationReportJSONLDEmitter
import org.scalatest
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class VocabularyDefinitionValidationTest
    extends AsyncFunSuite
    with Matchers
    with FileAssertionTest
    with PlatformSecrets {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/validations/vocabularies"

  test("Test missing vocabulary term") {
    validate("vocabulary.yaml", Some("validation.jsonld"), "missing-vocabulary-term")
  }

  test("Test missing base term") {
    validate("vocabulary.yaml", Some("validation.jsonld"), "missing-base-term")
  }

  test("Test repeated term in property terms and class terms") {
    validate("vocabulary.yaml", Some("validation.jsonld"), "repeated-term")
  }

  test("Test missing class term") {
    validate("vocabulary.yaml", Some("validation.jsonld"), "missing-class-term")
  }

  test("Test missing property term") {
    validate("vocabulary.yaml", Some("validation.jsonld"), "missing-property-term")
  }

  test("Validate vocabulary in JSON") {
    validate("vocabulary.json", None, "../../dialects/json/with-vocabulary")
  }

  test("Property term parsing with null entries uses errorHandler") {
    validate("property-term-with-null-entries.yaml", Some("property-term-with-null-entries.jsonld"), "parser-errors")
  }

  test("Class term parsing with null entries uses errorHandler") {
    validate("class-term-with-null-entries.yaml", Some("class-term-with-null-entries.jsonld"), "parser-errors")
  }

  protected def validate(
      vocabulary: String,
      goldenReport: Option[String] = None,
      path: String
  ): Future[scalatest.Assertion] = {
    val configuration = AMLConfiguration.predefined()
    val report = for {
      report <- configuration
        .baseUnitClient()
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
