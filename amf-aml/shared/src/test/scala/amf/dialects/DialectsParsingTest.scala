package amf.dialects

import amf.core.remote._
import amf.plugins.document.vocabularies.AMLPlugin

import scala.concurrent.ExecutionContext

trait DialectsParsingTest extends DialectTests {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/dialects/"

  multiGoldenTest("parse 1 test", "example1.%s") { config =>
    cycle("example1.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 1b test", "example1b.%s") { config =>
    cycle("example1b.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 2 test", "example2.%s") { config =>
    cycle("example2.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 3 test", "example3.%s") { config =>
    cycle("example3.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 4 test", "example4.%s") { config =>
    cycle("example4.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 5 test", "example5.%s") { config =>
    cycle("example5.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 6 test", "example6.%s") { config =>
    cycle("example6.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 7 test", "example7.%s") { config =>
    cycle("example7.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 8 test", "example8.%s") { config =>
    cycle("example8.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 9 test", "example9.%s") { config =>
    cycle("example9.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 10 test", "example10.%s") { config =>
    cycle("example10.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 11 test", "example11.%s") { config =>
    cycle("example11.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 12 test", "example12.%s") { config =>
    cycle("example12.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 13 test", "example13.%s") { config =>
    cycle("example13.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 14 test", "example14.%s") { config =>
    cycle("example14.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 15 test", "example15.%s") { config =>
    cycle("example15.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 16 test", "example16.%s") { config =>
    cycle("example16.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 17 test", "example17.%s") { config =>
    cycle("example17.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 18 test", "example18.%s") { config =>
    cycle("example18.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 19 test", "example19.%s") { config =>
    cycle("example19.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 20 test", "example20.%s") { config =>
    cycle("example20.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 21 test", "example21.%s") { config =>
    cycle("example21.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 22 test", "example22.%s") { config =>
    cycle("example22.yaml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 23a test", "example23a.%s") { config =>
    cycle("example23a.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 23b test", "example23b.%s") { config =>
    cycle("example23b.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse mappings_lib test", "mappings_lib.%s") { config =>
    cycle("mappings_lib.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions))
  }

  multiSourceTest("generate 1 test", "example1.%s") { config =>
    cycle(config.source, "example1.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 2 test", "example2.%s") { config =>
    cycle(config.source, "example2.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 3 test", "example3.%s") { config =>
    cycle(config.source, "example3.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 4 test", "example4.%s") { config =>
    cycle(config.source, "example4.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 5 test", "example5.%s") { config =>
    cycle(config.source, "example5.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 6 test", "example6.%s") { config =>
    cycle(config.source, "example6.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 7 test", "example7.%s") { config =>
    cycle(config.source, "example7.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 8 test", "example8.%s") { config =>
    cycle(config.source, "example8.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 9 test", "example9.%s") { config =>
    cycle(config.source, "example9.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 10 test", "example10.%s") { config =>
    cycle(config.source, "example10.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 11 test", "example11.%s") { config =>
    cycle(config.source, "example11.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 12 test", "example12.%s") { config =>
    cycle(config.source, "example12.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 13 test", "example13.%s") { config =>
    cycle(config.source, "example13.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 14 test", "example14.%s") { config =>
    cycle(config.source, "example14.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 15 test", "example15.%s") { config =>
    cycle(config.source, "example15.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 16 test", "example16.%s") { config =>
    cycle(config.source, "example16.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 17 test", "example17.%s") { config =>
    cycle(config.source, "example17.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 18 test", "example18.%s") { config =>
    cycle(config.source, "example18.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 19 test", "example19.%s") { config =>
    cycle(config.source, "example19.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 20 test", "example20.%s") { config =>
    cycle(config.source, "example20.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 21 test", "example21.%s") { config =>
    cycle(config.source, "example21.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 22 test", "example22.%s") { config =>
    cycle(config.source, "example22.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 23a test", "example23a.%s") { config =>
    cycle(config.source, "example23a.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 23b test", "example23b.%s") { config =>
    cycle(config.source, "example23b.yaml", AmfJsonHint, target = Aml)
  }

  multiGoldenTest("no documents on dialect (raml -> json)", "no-documents.%s") { config =>
    cycle("no-documents.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions))
  }

  multiSourceTest("no documents on dialect (json -> raml)", "no-documents.%s") { config =>
    cycle(config.source, "no-documents.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate mappings_lib test", "mappings_lib.%s") { config =>
    cycle(config.source, "mappings_lib.yaml", AmfJsonHint, target = Aml)
  }

  // Key Property tests
  multiGoldenTest("parse 19 test - with key property", "keyproperty/example19-keyproperty.%s") { config =>
    cycle("keyproperty/example19-keyproperty.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions))
  }

  multiSourceTest("generate 19 test - with key property", "keyproperty/example19-keyproperty.%s") { config =>
    cycle(config.source, "keyproperty/example19-keyproperty.yaml", AmfJsonHint, target = Aml)
  }

  // Reference Style tests
  multiGoldenTest("parse 19 test - with reference style", "referencestyle/example19-referencestyle.%s") { config =>
    cycle("referencestyle/example19-referencestyle.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions))
  }

  multiSourceTest("generate 19 test - with reference style", "referencestyle/example19-referencestyle.%s") { config =>
    cycle(config.source, "referencestyle/example19-referencestyle.yaml", AmfJsonHint, target = Aml)
  }

  test("generate 20 test - without version") {
    val preRegistry = AMLPlugin().registry.allDialects().size
    for {
      b <- parseAndRegisterDialect(s"file://$basePath/invalid/example20-no-version.yaml", platform, VocabularyYamlHint)
    } yield {
      assert(AMLPlugin().registry.allDialects().size == preRegistry)
      assert(AMLPlugin().registry.dialectById(b.id).isEmpty)
    }
  }

  test("generate 21 test - without name") {

    val preRegistry = AMLPlugin().registry.allDialects().size
    for {
      b <- parseAndRegisterDialect(s"file://$basePath/invalid/example21-no-name.yaml", platform, VocabularyYamlHint)
    } yield {
      assert(AMLPlugin().registry.allDialects().size == preRegistry)
      assert(AMLPlugin().registry.dialectById(b.id).isEmpty)
    }
  }

  multiGoldenTest("Parse dialect with fragment", "dialect.%s") { config =>
    cycle("dialect.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions),
          directory = s"$basePath/dialect-fragment")
  }

  multiGoldenTest("Parse dialect with library", "dialect.%s") { config =>
    cycle("dialect.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions),
          directory = s"$basePath/dialect-library")
  }
}
