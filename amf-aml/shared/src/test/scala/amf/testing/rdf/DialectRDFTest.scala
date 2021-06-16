package amf.testing.rdf

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.internal.remote.{Amf, Aml, VocabularyYamlHint}
import amf.core.internal.unsafe.PlatformSecrets
import amf.testing.common.cycling.FunSuiteRdfCycleTests
import amf.testing.common.utils.AMLParsingHelper

import scala.concurrent.ExecutionContext

class DialectRDFTest extends FunSuiteRdfCycleTests with PlatformSecrets with AMLParsingHelper {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  val productionPath                                       = "amf-aml/shared/src/test/resources/vocabularies2/production/"

  override def basePath: String = "amf-aml/shared/src/test/resources/vocabularies2/dialects/"

  test("RDF 1 test") {
    cycleFullRdf("example1.yaml", "example1.yaml", VocabularyYamlHint, Aml, basePath)
  }

  test("RDF 2 test") {
    cycleFullRdf("example2.yaml", "example2.yaml", VocabularyYamlHint, Aml, basePath)
  }

  multiGoldenTest("RDF 3 test", "example3.rdf-cycled.%s") { config =>
    cycleFullRdf("example3.yaml",
                 config.golden,
                 VocabularyYamlHint,
                 target = Amf,
                 directory = basePath,
                 AMLConfiguration.predefined().withRenderOptions(config.renderOptions))
  }

  multiGoldenTest("RDF 13 test", "example13.rdf-cycled.%s") { config =>
    cycleFullRdf("example13.yaml",
                 config.golden,
                 VocabularyYamlHint,
                 target = Amf,
                 directory = basePath,
                 AMLConfiguration.predefined().withRenderOptions(config.renderOptions))
  }

  multiGoldenTest("RDF Production system2 dialect ex1  test", "dialectex1.%s") { config =>
    cycleFullRdf(
        "dialectex1.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = s"${productionPath}system2/",
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions)
    )
  }

  multiGoldenTest("RDF Production system2 dialect ex2  test", "dialectex2.%s") { config =>
    cycleFullRdf(
        "dialectex2.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = s"${productionPath}system2/",
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions)
    )
  }

  override def defaultRenderOptions: RenderOptions = RenderOptions().withSourceMaps.withPrettyPrint
}
