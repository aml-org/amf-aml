package amf.dialects

import amf.client.parse.DefaultParserErrorHandler
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.core.remote._
import amf.core.unsafe.PlatformSecrets
import amf.core.io.FunSuiteRdfCycleTests
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.features.validation.AMFValidatorPlugin

import scala.concurrent.Future

class DialectInstancesRDFTest extends FunSuiteRdfCycleTests with PlatformSecrets with DefaultAmfInitialization {

  val basePath       = "amf-aml/shared/src/test/resources/vocabularies2/instances/"
  val productionPath = "amf-aml/shared/src/test/resources/vocabularies2/production/"

  test("RDF 1 test") {
    withDialect("dialect1.yaml", "example1.yaml", "example1.ttl", VocabularyYamlHint, Amf)
  }

  test("RDF 2 full test") {
    withDialectFull("dialect2.yaml", "example2.yaml", "example2.yaml", VocabularyYamlHint, Aml)
  }

  ignore("RDF 3 full test") {
    withDialectFull("dialect3.yaml", "example3.yaml", "example3.yaml", VocabularyYamlHint, Aml)
  }

  test("RDF 4 full test") {
    withDialectFull("dialect4.yaml", "example4.yaml", "example4.yaml", VocabularyYamlHint, Aml)
  }

  test("RDF 5 full test") {
    withDialectFull("dialect5.yaml", "example5.yaml", "example5.yaml", VocabularyYamlHint, Aml)
  }

  test("RDF 6 full test") {
    withDialectFull("dialect6.yaml", "example6.yaml", "example6.yaml", VocabularyYamlHint, Aml)
  }

  test("RDF 26 full test") {
    withDialectFull("dialect26.yaml", "example26.yaml", "example26.yaml", VocabularyYamlHint, Aml)
  }

  test("RDF 1 Vocabulary full test") {
    cycleFullRdf("example1.yaml",
                 "example1.yaml",
                 VocabularyYamlHint,
                 Aml,
                 "amf-aml/shared/src/test/resources/vocabularies2/vocabularies/")
  }

  test("RDF 1 Dialect full test") {
    cycleFullRdf("example1.yaml",
                 "example1.yaml",
                 VocabularyYamlHint,
                 Aml,
                 "amf-aml/shared/src/test/resources/vocabularies2/dialects/")
  }

  test("EngDemos vocabulary test") {
    cycleFullRdf("eng_demos.yaml",
                 "eng_demos.yaml",
                 VocabularyYamlHint,
                 Aml,
                 "amf-aml/shared/src/test/resources/vocabularies2/production/")
  }

  test("Container Configuration 0.2 ex1 test") {
    withDialectFull("dialect.yaml",
                    "ex1.yaml",
                    "ex1.yaml",
                    VocabularyYamlHint,
                    Aml,
                    "amf-aml/shared/src/test/resources/vocabularies2/production/system2/")
  }

  test("Container Configuration 0.2 ex2 test") {
    withDialectFull("dialect.yaml",
                    "ex2.yaml",
                    "ex2.yaml",
                    VocabularyYamlHint,
                    Aml,
                    "amf-aml/shared/src/test/resources/vocabularies2/production/system2/")
  }

  private def withDialect(dialect: String,
                          source: String,
                          golden: String,
                          hint: Hint,
                          target: Vendor,
                          directory: String = basePath) = {

    val context =
      new CompilerContextBuilder(s"file://$directory/$dialect", platform, DefaultParserErrorHandler.withRun()).build()
    for {
      _   <- new AMFCompiler(context, None, Some(Aml.name)).build()
      res <- cycleRdf(source, golden, hint, target)
    } yield {
      res
    }
  }

  private def withDialectFull(dialect: String,
                              source: String,
                              golden: String,
                              hint: Hint,
                              target: Vendor,
                              directory: String = basePath) = {
    val context =
      new CompilerContextBuilder(s"file://$directory/$dialect", platform, DefaultParserErrorHandler.withRun()).build()
    for {
      _   <- new AMFCompiler(context, None, Some(Aml.name)).build()
      res <- cycleFullRdf(source, golden, hint, target, directory)
    } yield {
      res
    }
  }
}
