package amf.testing.rdf

import amf.client.parse.DefaultParserErrorHandler
import amf.core.remote._
import amf.core.unsafe.PlatformSecrets
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.testing.common.cycling.FunSuiteRdfCycleTests
import amf.testing.common.utils.{DefaultAMLInitialization, DialectRegistrationHelper}

class DialectInstancesRDFTest
    extends FunSuiteRdfCycleTests
    with PlatformSecrets
    with DefaultAMLInitialization
    with DialectRegistrationHelper {

  val basePath       = "amf-aml/shared/src/test/resources/vocabularies2/instances/"
  val productionPath = "amf-aml/shared/src/test/resources/vocabularies2/production/"

  test("RDF 1 test") {
    cycleRdfWithDialect("dialect1.yaml", "example1.yaml", "example1.ttl", VocabularyYamlHint, Amf)
  }

  test("RDF 2 full test") {
    cycleFullRdfWithDialect("dialect2.yaml", "example2.yaml", "example2.yaml", VocabularyYamlHint, Aml)
  }

  ignore("RDF 3 full test") {
    cycleFullRdfWithDialect("dialect3.yaml", "example3.yaml", "example3.yaml", VocabularyYamlHint, Aml)
  }

  test("RDF 4 full test") {
    cycleFullRdfWithDialect("dialect4.yaml", "example4.yaml", "example4.yaml", VocabularyYamlHint, Aml)
  }

  test("RDF 5 full test") {
    cycleFullRdfWithDialect("dialect5.yaml", "example5.yaml", "example5.yaml", VocabularyYamlHint, Aml)
  }

  test("RDF 6 full test") {
    cycleFullRdfWithDialect("dialect6.yaml", "example6.yaml", "example6.yaml", VocabularyYamlHint, Aml)
  }

  test("RDF 26 full test") {
    cycleFullRdfWithDialect("dialect26.yaml", "example26.yaml", "example26.yaml", VocabularyYamlHint, Aml)
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
    cycleFullRdfWithDialect("dialect.yaml",
                            "ex1.yaml",
                            "ex1.yaml",
                            VocabularyYamlHint,
                            Aml,
                            "amf-aml/shared/src/test/resources/vocabularies2/production/system2/")
  }

  test("Container Configuration 0.2 ex2 test") {
    cycleFullRdfWithDialect("dialect.yaml",
                            "ex2.yaml",
                            "ex2.yaml",
                            VocabularyYamlHint,
                            Aml,
                            "amf-aml/shared/src/test/resources/vocabularies2/production/system2/")
  }

  private def cycleRdfWithDialect(dialect: String,
                                  source: String,
                                  golden: String,
                                  hint: Hint,
                                  target: Vendor,
                                  directory: String = basePath) = {

    withDialect(s"file://$directory/$dialect") { _ =>
      cycleRdf(source, golden, hint, target)
    }
  }

  private def cycleFullRdfWithDialect(dialect: String,
                                      source: String,
                                      golden: String,
                                      hint: Hint,
                                      target: Vendor,
                                      directory: String = basePath) = {
    withDialect(s"file://$directory/$dialect") { _ =>
      cycleFullRdf(source, golden, hint, target, directory)
    }
  }
}
