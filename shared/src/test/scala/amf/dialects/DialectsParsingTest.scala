package amf.dialects

import amf.core.remote._
import amf.plugins.document.vocabularies.AMLPlugin

import scala.concurrent.ExecutionContext

trait DialectsParsingTest extends DialectTests {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "shared/src/test/resources/vocabularies2/dialects/"

  multiGoldenTest("parse 1 test", "example1.%s") { config =>
    init().flatMap(_ => cycle("example1.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 1b test", "example1b.%s") { config =>
    init().flatMap(_ => cycle("example1b.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 2 test", "example2.%s") { config =>
    init().flatMap(_ => cycle("example2.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 3 test", "example3.%s") { config =>
    init().flatMap(_ => cycle("example3.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 4 test", "example4.%s") { config =>
    init().flatMap(_ => cycle("example4.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 5 test", "example5.%s") { config =>
    init().flatMap(_ => cycle("example5.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 6 test", "example6.%s") { config =>
    init().flatMap(_ => cycle("example6.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 7 test", "example7.%s") { config =>
    init().flatMap(_ => cycle("example7.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 8 test", "example8.%s") { config =>
    init().flatMap(_ => cycle("example8.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 9 test", "example9.%s") { config =>
    init().flatMap(_ => cycle("example9.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 10 test", "example10.%s") { config =>
    init().flatMap(_ => cycle("example10.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 11 test", "example11.%s") { config =>
    init().flatMap(_ => cycle("example11.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 12 test", "example12.%s") { config =>
    init().flatMap(_ => cycle("example12.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 13 test", "example13.%s") { config =>
    init().flatMap(_ => cycle("example13.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 14 test", "example14.%s") { config =>
    init().flatMap(_ => cycle("example14.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 15 test", "example15.%s") { config =>
    init().flatMap(_ => cycle("example15.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 16 test", "example16.%s") { config =>
    init().flatMap(_ => cycle("example16.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 17 test", "example17.%s") { config =>
    init().flatMap(_ => cycle("example17.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 18 test", "example18.%s") { config =>
    init().flatMap(_ => cycle("example18.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 19 test", "example19.%s") { config =>
    init().flatMap(_ => cycle("example19.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 20 test", "example20.%s") { config =>
    init().flatMap(_ => cycle("example20.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 21 test", "example21.%s") { config =>
    init().flatMap(_ => cycle("example21.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 22 test", "example22.%s") { config =>
    init().flatMap(_ => cycle("example22.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 23a test", "example23a.%s") { config =>
    init().flatMap(_ => cycle("example23a.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 23b test", "example23b.%s") { config =>
    init().flatMap(_ => cycle("example23b.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse mappings_lib test", "mappings_lib.%s") { config =>
    init().flatMap(_ => cycle("mappings_lib.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiSourceTest("generate 1 test", "example1.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example1.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 2 test", "example2.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example2.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 3 test", "example3.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example3.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 4 test", "example4.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example4.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 5 test", "example5.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example5.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 6 test", "example6.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example6.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 7 test", "example7.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example7.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 8 test", "example8.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example8.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 9 test", "example9.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example9.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 10 test", "example10.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example10.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 11 test", "example11.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example11.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 12 test", "example12.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example12.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 13 test", "example13.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example13.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 14 test", "example14.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example14.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 15 test", "example15.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example15.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 16 test", "example16.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example16.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 17 test", "example17.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example17.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 18 test", "example18.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example18.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 19 test", "example19.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example19.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 20 test", "example20.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example20.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 21 test", "example21.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example21.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 22 test", "example22.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example22.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 23a test", "example23a.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example23a.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 23b test", "example23b.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example23b.raml", AmfJsonHint, target = Aml))
  }

  multiGoldenTest("no documents on dialect (raml -> json)", "no-documents.%s") { config =>
    init().flatMap(_ => cycle("no-documents.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiSourceTest("no documents on dialect (json -> raml)", "no-documents.%s") { config =>
    init().flatMap(_ => cycle(config.source, "no-documents.raml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate mappings_lib test", "mappings_lib.%s") { config =>
    init().flatMap(_ => cycle(config.source, "mappings_lib.raml", AmfJsonHint, target = Aml))
  }

  // Key Property tests
  multiGoldenTest("parse 19 test - with key property", "keyproperty/example19-keyproperty.%s") { config =>
    init().flatMap(_ => cycle("keyproperty/example19-keyproperty.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiSourceTest("generate 19 test - with key property", "keyproperty/example19-keyproperty.%s") { config =>
    init().flatMap(_ => cycle(config.source, "keyproperty/example19-keyproperty.raml", AmfJsonHint, target = Aml))
  }

  // Reference Style tests
  multiGoldenTest("parse 19 test - with reference style", "referencestyle/example19-referencestyle.%s") { config =>
      init().flatMap(_ => cycle("referencestyle/example19-referencestyle.raml", config.golden, VocabularyYamlHint, target = Amf, renderOptions = Some(config.renderOptions)))
  }

  multiSourceTest("generate 19 test - with reference style", "referencestyle/example19-referencestyle.%s") { config =>
    init().flatMap(_ => cycle(config.source, "referencestyle/example19-referencestyle.raml", AmfJsonHint, target = Aml))
  }

  test("generate 20 test - without version") {
    val preRegistry = AMLPlugin().registry.allDialects().size
    for {
      b <- parseAndRegisterDialect(s"file://$basePath/invalid/example20-no-version.raml", platform, VocabularyYamlHint)
    } yield {
      assert(AMLPlugin().registry.allDialects().size == preRegistry)
      assert(AMLPlugin().registry.dialectById(b.id).isEmpty)
    }
  }

  test("generate 21 test - without name") {

    val preRegistry = AMLPlugin().registry.allDialects().size
    for {
      b <- parseAndRegisterDialect(s"file://$basePath/invalid/example21-no-name.raml", platform, VocabularyYamlHint)
    } yield {
      assert(AMLPlugin().registry.allDialects().size == preRegistry)
      assert(AMLPlugin().registry.dialectById(b.id).isEmpty)
    }
  }
}
