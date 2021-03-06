package amf.dialects

import amf.ProfileName
import org.scalatest.Assertion

import scala.concurrent.{ExecutionContext, Future}

trait DialectInstancesValidationTest extends DialectInstanceValidation with DefaultAmfInitialization {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath                                   = "file://amf-aml/shared/src/test/resources/vocabularies2/validation"
  val productionPath                             = "file://amf-aml/shared/src/test/resources/vocabularies2/production"
  private val reportComparator: ReportComparator = UniquePlatformReportComparator

  def validate(dialect: String,
               instance: String,
               golden: Option[String] = None,
               path: String = basePath): Future[Assertion] = {
    validation(dialect, instance, path) flatMap {
      reportComparator.assertReport(_, golden.map(g => s"$path/$g"))
    }
  }

  test("validation dialect 1 example 1 correct") {
    validate("dialect1.yaml", "instance1_correct1.yaml")
  }

  test("validation of dialect with any type") {
    validate("any_dialect.yaml", "any_example.yaml")
  }

  test("validation of dialect with any type array") {
    validate("any_array_dialect.yaml", "any_array_example.yaml")
  }

  test("validation dialect 1b example 1b correct") {
    validate("dialect1b.yaml", "example1b.yaml")
  }

  test("validation dialect 1 example 1 incorrect") {
    validate("dialect1.yaml", "instance1_incorrect1.yaml", Some("instance1_incorrect1.report.json"))
  }

  test("validation dialect 2 example 1 correct") {
    validate("dialect2.yaml", "instance2_correct1.yaml")
  }

  test("validation dialect 2 example 1 incorrect") {
    validate("dialect2.yaml", "instance2_incorrect1.yaml", Some("instance2_incorrect1.report.json"))
  }

  test("validation dialect 3 example 1 correct") {
    validate("dialect3.yaml", "instance3_correct1.yaml")
  }

  test("validation dialect 3 example 1 incorrect") {
    validate("dialect3.yaml", "instance3_incorrect1.yaml", Some("instance3_incorrect1.report.json"))
  }

  test("validation dialect 4 example 1 correct") {
    validate("dialect4.yaml", "instance4_correct1.yaml")
  }

  test("validation dialect 5 example 1 correct") {
    validate("dialect5.yaml", "instance5_correct1.yaml")
  }

  test("validation dialect 5 example 1 incorrect") {
    validate("dialect5.yaml", "instance5_incorrect1.yaml", Some("instance5_incorrect1.report.json"))
  }

  test("validation dialect 6 example 1 correct") {
    validate("dialect6.yaml", "instance6_correct1.yaml")
  }

  test("validation dialect 6 example 1 incorrect") {
    validate("dialect6.yaml", "instance6_incorrect1.yaml", Some("instance6_incorrect1.report.json"))
  }

  test("validation dialect 7 example 1 correct") {
    validate("dialect7.yaml", "instance7_correct1.yaml")
  }

  test("validation dialect 7 example 1 incorrect") {
    validate("dialect7.yaml", "instance7_incorrect1.yaml", Some("instance7_incorrect1.report.json"))
  }

  test("validation dialect 8 example 1 correct") {
    validate("dialect8a.yaml", "instance8_correct1.yaml")
  }

  test("validation dialect 8 example 1 incorrect") {
    validate("dialect8a.yaml", "instance8_incorrect1.yaml", Some("instance8_incorrect1.report.json"))
  }

  test("validation dialect 9 example 1 correct") {
    validate("dialect9.yaml", "instance9_correct1.yaml")
  }

  test("validation dialect 9 example 1 incorrect") {
    validate("dialect9.yaml", "instance9_incorrect1.yaml", Some("instance9_incorrect1.report.json"))
  }

  test("validation dialect 10 example 1 correct") {
    validate("dialect10.yaml", "instance10_correct1.yaml")
  }

  // TODO: un-ignore when AML re-implements the validation
  ignore("validation dialect 10 example 1 incorrect - Dialect ID in comment (instead of key)") {
    recoverToSucceededIf[Exception] { // Unknown type of dialect header
      validate("dialect10.yaml", "instance10_incorrect1.yaml")
    }
  }

  // TODO: un-ignore when AML re-implements the validation
  ignore("validation dialect 10 example 2 incorrect - Dialect ID in both key and comment") {
    validate("dialect9.yaml", "instance9_correct1.yaml").flatMap(_ =>
      validate("dialect10.yaml", "instance10_incorrect2.yaml"))
    // 1st error -> Dialect 9 defined in Header and Dialect 10 as key (validation and parse Dialect 9 as fallback)
    // 2nd error -> Dialect 9 does not accepts Dialect 10 key as a Root declaration
  }

  test("validation mule_config  example 1 correct") {
    validate("mule_config_dialect1.yaml", "mule_config_instance_correct1.yaml")
  }

  test("validation mule_config  example 2 incorrect") {
    validate("mule_config_dialect1.yaml",
             "mule_config_instance_incorrect2.yaml",
             Some("mule_config_instance_incorrect2.report.json"))
  }

  test("validation eng_demos  example 1 correct") {
    validate("eng_demos_dialect1.yaml", "eng_demos_instance1.yaml")
  }

  test("Can validate asyncapi 0.1 error") {
    validate("dialect1.yaml",
             "example1.yaml",
             path = s"$productionPath/asyncapi",
             golden = Some("example1.report.json"))
  }

  test("Can validate asyncapi 0.2 correct") {
    validate("dialect2.yaml", "example2.yaml", path = s"$productionPath/asyncapi")
  }

  test("Can validate asyncapi 0.3 correct") {
    validate("dialect3.yaml", "example3.yaml", path = s"$productionPath/asyncapi")
  }

  test("Can validate asyncapi 0.4 correct") {
    validate("dialect4.yaml", "example4.yaml", path = s"$productionPath/asyncapi")
  }

  test("Can validate container configurations") {
    validate("dialect.yaml", "system.yaml", path = s"$productionPath/system")
  }

  test("Can validate oas 2.0 dialect instances") {
    validate("oas20_dialect1.yaml", "oas20_instance1.yaml", path = productionPath)
  }

  test("Can validate multiple property values with mapTermKey property") {
    validate("map-term-key.yaml", "map-term-key-instance.yaml")
  }

  test("validation dialect 11 example 1 correct") {
    validate("dialect11.yaml", "instance11_correct1.yaml")
  }

  test("Validate native link with colliding properties") {
    validate("dialect.yaml",
             "instance.yaml",
             Some("report.json"),
             path = s"$basePath/native-link-with-colliding-properties")
  }

  // This should be invalid!
  test("Validate native link with extra properties") {
    validate("dialect.yaml",
             "instance.yaml",
             Some("report.json"),
             path = s"$basePath/native-link-with-extra-properties")
  }

  // This should be invalid!
  test("Validate native link with $base without $id") {
    validate("dialect.yaml",
             "instance.yaml",
             Some("report.json"),
             path = s"$basePath/native-link-with-$$base-without-$$id")
  }

  test("Validate additional nodes in union node") {
    validate("dialect.yaml", "instance.yaml", Some("report.json"), path = s"$basePath/additional-nodes-in-union-node")
  }

}
