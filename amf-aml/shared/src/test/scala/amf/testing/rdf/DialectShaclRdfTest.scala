package amf.testing.rdf

import amf.client.environment.AMLConfiguration
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.rdf.RdfModel
import amf.core.remote.Syntax.Syntax
import amf.core.remote.{Amf, Hint, Vendor, VocabularyYamlHint}
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.testing.common.cycling.FunSuiteRdfCycleTests
import amf.testing.common.utils.{AMLParsingHelper, DefaultAMLInitialization}
import org.scalatest.Assertion

import scala.concurrent.{ExecutionContext, Future}

class DialectShaclRdfTest
    extends FunSuiteRdfCycleTests
    with PlatformSecrets
    with AMLParsingHelper
    with DefaultAMLInitialization {

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
    AMLPlugin().shapesForDialect(unit.asInstanceOf[Dialect], "http://metadata.org/validations.js")
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
                        hint: Hint = VocabularyYamlHint,
                        target: Vendor = Amf,
                        amlConfig: AMLConfiguration =
                          AMLConfiguration.predefined().withErrorHandlerProvider(() => UnhandledErrorHandler),
                        directory: String = basePath,
                        syntax: Option[Syntax] = None,
                        pipeline: Option[String] = None,
                        transformWith: Option[Vendor] = None): Future[Assertion] = {

    val config = CycleConfig(source, golden, hint, target, directory, syntax, pipeline)

    build(config, amlConfig, useAmfJsonldSerialisation = true)
      .map(transformRdf(_, config))
      .flatMap(renderRdf(_, config))
      .flatMap(writeTemporaryFile(golden))
      .flatMap(assertDifferences(_, config.goldenPath))
  }
}
