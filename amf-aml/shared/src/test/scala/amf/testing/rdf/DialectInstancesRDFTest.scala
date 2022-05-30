package amf.testing.rdf

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.rdf.client.scala.{RdfFramework, RdfUnitConverter}
import amf.rdf.internal.RdfFrameworkBuilder
import amf.core.internal.remote.Syntax.Syntax
import amf.core.internal.remote._
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.io.FileAssertionTest
import amf.testing.common.cycling.FunSuiteRdfCycleTests
import amf.testing.common.utils.DialectRegistrationHelper

import scala.concurrent.Future

class DialectInstancesRDFTest extends FunSuiteRdfCycleTests with FileAssertionTest with DialectRegistrationHelper {

  val basePath                          = "amf-aml/shared/src/test/resources/vocabularies2/instances/"
  val productionPath                    = "amf-aml/shared/src/test/resources/vocabularies2/production/"
  protected val framework: RdfFramework = RdfFrameworkBuilder.build()

  test("RDF 1 test") {
    cycleRdfWithDialect("dialect1.yaml", "example1.yaml", "example1.ttl", None)
  }

  test("RDF 2 full test") {
    cycleFullRdfWithDialect("dialect2.yaml", "example2.yaml", "example2.yaml", syntax = Some(Syntax.Yaml))
  }

  ignore("RDF 3 full test") {
    cycleFullRdfWithDialect("dialect3.yaml", "example3.yaml", "example3.yaml", syntax = Some(Syntax.Yaml))
  }

  test("RDF 4 full test") {
    cycleFullRdfWithDialect("dialect4.yaml", "example4.yaml", "example4.yaml", syntax = Some(Syntax.Yaml))
  }

  test("RDF 5 full test") {
    cycleFullRdfWithDialect("dialect5.yaml", "example5.yaml", "example5.yaml", syntax = Some(Syntax.Yaml))
  }

  test("RDF 6 full test") {
    cycleFullRdfWithDialect("dialect6.yaml", "example6.yaml", "example6.yaml", syntax = Some(Syntax.Yaml))
  }

  test("RDF 26 full test") {
    cycleFullRdfWithDialect("dialect26.yaml", "example26.yaml", "example26.yaml", syntax = Some(Syntax.Yaml))
  }

  test("RDF 1 Vocabulary full test") {
    cycleFullRdf(
        "example1.yaml",
        "example1.yaml",
        "amf-aml/shared/src/test/resources/vocabularies2/vocabularies/",
        syntax = Some(Syntax.Yaml)
    )
  }

  test("RDF 1 Dialect full test") {
    cycleFullRdf(
        "example1.yaml",
        "example1.yaml",
        "amf-aml/shared/src/test/resources/vocabularies2/dialects/",
        syntax = Some(Syntax.Yaml)
    )
  }

  test("EngDemos vocabulary test") {
    cycleFullRdf(
        "eng_demos.yaml",
        "eng_demos.yaml",
        "amf-aml/shared/src/test/resources/vocabularies2/production/",
        syntax = Some(Syntax.Yaml)
    )
  }

  test("Container Configuration 0.2 ex1 test") {
    cycleFullRdfWithDialect(
        "dialect.yaml",
        "ex1.yaml",
        "ex1.yaml",
        syntax = Some(Syntax.Yaml),
        "amf-aml/shared/src/test/resources/vocabularies2/production/system2/"
    )
  }

  test("Container Configuration 0.2 ex2 test") {
    cycleFullRdfWithDialect(
        "dialect.yaml",
        "ex2.yaml",
        "ex2.yaml",
        syntax = Some(Syntax.Yaml),
        "amf-aml/shared/src/test/resources/vocabularies2/production/system2/"
    )
  }

  test("Dialect instance with array of any") {
    val basePath     = "amf-aml/shared/src/test/resources/vocabularies2/instances/array-of-any"
    val dialectPath  = s"file://$basePath/dialect.yaml"
    val instancePath = s"$basePath/instance.json"

    withDialect(dialectPath) { (_, config) =>
      for {
        instanceText <- fs.asyncFile(instancePath).read()
        actualText <- Future.successful {
          // Cycle: JSON-LD -> RDF model -> Base Unit -> JSON-LD
          val rdfDocument = framework.syntaxToRdfModel(Mimes.`application/ld+json`, instanceText)
          val baseUnit = {
            val id = s"file://$instancePath"
            RdfUnitConverter.fromNativeRdfModel(id, rdfDocument.model, config)
          }
          config
            .withRenderOptions(RenderOptions().withCompactUris.withPrettyPrint)
            .baseUnitClient()
            .render(baseUnit, Mimes.`application/ld+json`)
        }
        actualFile <- writeTemporaryFile(instancePath)(actualText)
        assertion  <- assertDifferences(actualFile, instancePath.replace(".json", ".golden.json"))
      } yield {
        assertion
      }
    }
  }

  private def cycleRdfWithDialect(
      dialect: String,
      source: String,
      golden: String,
      syntax: Option[Syntax],
      directory: String = basePath
  ) = {

    withDialect(s"file://$directory/$dialect") { (_, config) =>
      cycleRdf(source, golden, config, syntax = syntax)
    }
  }

  private def cycleFullRdfWithDialect(
      dialect: String,
      source: String,
      golden: String,
      syntax: Option[Syntax],
      directory: String = basePath
  ) = {
    withDialect(s"file://$directory/$dialect") { (_, config) =>
      cycleFullRdf(source, golden, directory, config, syntax)
    }
  }
}
