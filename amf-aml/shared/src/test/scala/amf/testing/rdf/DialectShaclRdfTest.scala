package amf.testing.rdf

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.rdf.RdfModel
import amf.core.internal.remote.Syntax.Syntax
import amf.core.internal.remote.{Amf, Hint, Spec, VocabularyYamlHint}
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.internal.validation.CoreValidations
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.aml.client.scala.model.document.Dialect
import amf.aml.internal.validate.{AMFDialectValidations, DialectValidations}
import amf.testing.common.cycling.FunSuiteRdfCycleTests
import amf.testing.common.utils.AMLParsingHelper
import amf.validation.internal.PlatformValidator
import org.scalatest.Assertion

import scala.concurrent.{ExecutionContext, Future}

class DialectShaclRdfTest extends FunSuiteRdfCycleTests with PlatformSecrets with AMLParsingHelper {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/dialects/"

  test("shacl 1 test") {
    cycleRdf("example1.yaml", s"example1.shacl")
  }

  test("generate validations for unreferenced node mappings") {
    cycleRdf("dialect.yaml", s"validations.shacl", directory = s"${basePath}unreferenced-node-mappings-validations/")
  }

  /** Method for transforming parsed unit. Override if necessary. */
  override def transformRdf(unit: BaseUnit, config: CycleConfig): RdfModel = {
    val finder            = DefaultNodeMappableFinder.empty()
    val validationProfile = new AMFDialectValidations(unit.asInstanceOf[Dialect])(finder).profile()
    val validations = validationProfile.validations.filter(v =>
      !DialectValidations.validations.contains(v) && !CoreValidations.validations.contains(v))
    PlatformValidator.instance(Seq.empty).shapes(validations, "http://metadata.org/validations.js")
  }

  /** Method to render parsed unit. Override if necessary. */
  override def renderRdf(unit: RdfModel, config: CycleConfig): Future[String] = {
    Future {
      unit.toN3().split("\n").sorted.mkString("\n")
    }
  }

  /** Compile source with specified hint. Render to temporary file and assert against golden. */
  override def cycleRdf(source: String,
                        golden: String,
                        amlConfig: AMLConfiguration =
                          AMLConfiguration.predefined().withErrorHandlerProvider(() => UnhandledErrorHandler),
                        directory: String = basePath,
                        syntax: Option[Syntax] = None,
                        pipeline: Option[String] = None,
                        transformWith: Option[Spec] = None): Future[Assertion] = {

    val config = CycleConfig(source, golden, directory, syntax, pipeline)

    build(config, amlConfig)
      .map(transformRdf(_, config))
      .flatMap(renderRdf(_, config))
      .flatMap(writeTemporaryFile(golden))
      .flatMap(assertDifferences(_, config.goldenPath))
  }
}
