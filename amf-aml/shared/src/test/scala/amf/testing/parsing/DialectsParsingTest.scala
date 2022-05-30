package amf.testing.parsing

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.internal.remote.Mimes
import amf.testing.common.utils.DialectTests

import scala.concurrent.ExecutionContext

trait DialectsParsingTest extends DialectTests {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/dialects/"

  private def multiCycleTest(label: String, directory: String): Unit = {

    multiGoldenTest(s"$label parsing from YAML to JSON-LD", "dialect.%s") { config =>
      cycle(
          "dialect.yaml",
          config.golden,
          Some(Mimes.`application/ld+json`),
          AMLConfiguration.predefined().withRenderOptions(config.renderOptions.withCompactUris),
          directory = directory
      )
    }

    multiSourceTest(s"$label parsing from JSON-LD to YAML", "dialect.%s") { config =>
      cycle(
          config.source,
          "dialect.cycled.jsonld.yaml",
          mediaType = Some(Mimes.`application/yaml`),
          directory = directory
      )
    }

    test(s"$label parsing from YAML to YAML") {
      cycle("dialect.yaml", "dialect.cycled.yaml", Some(Mimes.`application/yaml`), directory = directory)
    }

  }

  multiGoldenTest("parse 1 test", "example1.%s") { config =>
    cycle("example1.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 1b test", "example1b.%s") { config =>
    cycle("example1b.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 2 test", "example2.%s") { config =>
    cycle("example2.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 3 test", "example3.%s") { config =>
    cycle("example3.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 4 test", "example4.%s") { config =>
    cycle("example4.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 5 test", "example5.%s") { config =>
    cycle("example5.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 6 test", "example6.%s") { config =>
    cycle("example6.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 7 test", "example7.%s") { config =>
    cycle("example7.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 8 test", "example8.%s") { config =>
    cycle("example8.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 9 test", "example9.%s") { config =>
    cycle("example9.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 10 test", "example10.%s") { config =>
    cycle("example10.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 11 test", "example11.%s") { config =>
    cycle("example11.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 12 test", "example12.%s") { config =>
    cycle("example12.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 13 test", "example13.%s") { config =>
    cycle("example13.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 14 test", "example14.%s") { config =>
    cycle("example14.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 15 test", "example15.%s") { config =>
    cycle("example15.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 16 test", "example16.%s") { config =>
    cycle("example16.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 17 test", "example17.%s") { config =>
    cycle("example17.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 18 test", "example18.%s") { config =>
    cycle("example18.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 19 test", "example19.%s") { config =>
    cycle("example19.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 20 test", "example20.%s") { config =>
    cycle("example20.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 21 test", "example21.%s") { config =>
    cycle("example21.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 22 test", "example22.%s") { config =>
    cycle("example22.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 23a test", "example23a.%s") { config =>
    cycle("example23a.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 23b test", "example23b.%s") { config =>
    cycle("example23b.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse 24 test", "example24.%s") { config =>
    cycle("example24.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiGoldenTest("parse mappings_lib test", "mappings_lib.%s") { config =>
    cycle("mappings_lib.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiSourceTest("generate 1 test", "example1.%s") { config =>
    cycle(config.source, "example1.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 2 test", "example2.%s") { config =>
    cycle(config.source, "example2.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 3 test", "example3.%s") { config =>
    cycle(config.source, "example3.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 4 test", "example4.%s") { config =>
    cycle(config.source, "example4.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 5 test", "example5.%s") { config =>
    cycle(config.source, "example5.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 6 test", "example6.%s") { config =>
    cycle(config.source, "example6.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 7 test", "example7.%s") { config =>
    cycle(config.source, "example7.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 8 test", "example8.%s") { config =>
    cycle(config.source, "example8.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 9 test", "example9.%s") { config =>
    cycle(config.source, "example9.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 10 test", "example10.%s") { config =>
    cycle(config.source, "example10.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 11 test", "example11.%s") { config =>
    cycle(config.source, "example11.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 12 test", "example12.%s") { config =>
    cycle(config.source, "example12.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 13 test", "example13.%s") { config =>
    cycle(config.source, "example13.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 14 test", "example14.%s") { config =>
    cycle(config.source, "example14.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 15 test", "example15.%s") { config =>
    cycle(config.source, "example15.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 16 test", "example16.%s") { config =>
    cycle(config.source, "example16.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 17 test", "example17.%s") { config =>
    cycle(config.source, "example17.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 18 test", "example18.%s") { config =>
    cycle(config.source, "example18.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 19 test", "example19.%s") { config =>
    cycle(config.source, "example19.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 20 test", "example20.%s") { config =>
    cycle(config.source, "example20.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 21 test", "example21.%s") { config =>
    cycle(config.source, "example21.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 22 test", "example22.%s") { config =>
    cycle(config.source, "example22.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 23a test", "example23a.%s") { config =>
    cycle(config.source, "example23a.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 23b test", "example23b.%s") { config =>
    cycle(config.source, "example23b.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 24 test", "example24.%s") { config =>
    cycle(config.source, "example24.yaml", Some(Mimes.`application/yaml`))
  }

  multiGoldenTest("no documents on dialect (raml -> json)", "no-documents.%s") { config =>
    cycle("no-documents.yaml", config.golden, Some(Mimes.`application/ld+json`), amlConfig = config.config)
  }

  multiSourceTest("no documents on dialect (json -> raml)", "no-documents.%s") { config =>
    cycle(config.source, "no-documents.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate mappings_lib test", "mappings_lib.%s") { config =>
    cycle(config.source, "mappings_lib.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  // Key Property tests
  multiGoldenTest("parse 19 test - with key property", "keyproperty/example19-keyproperty.%s") { config =>
    cycle(
        "keyproperty/example19-keyproperty.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        amlConfig = config.config
    )
  }

  multiSourceTest("generate 19 test - with key property", "keyproperty/example19-keyproperty.%s") { config =>
    cycle(config.source, "keyproperty/example19-keyproperty.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  // Reference Style tests
  multiGoldenTest("parse 19 test - with reference style", "referencestyle/example19-referencestyle.%s") { config =>
    cycle(
        "referencestyle/example19-referencestyle.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        amlConfig = config.config
    )
  }

  multiSourceTest("generate 19 test - with reference style", "referencestyle/example19-referencestyle.%s") { config =>
    cycle(config.source, "referencestyle/example19-referencestyle.yaml", mediaType = Some(Mimes.`application/yaml`))
  }

  multiGoldenTest("Parse dialect with fragment", "dialect.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        directory = s"$basePath/dialect-fragment",
        amlConfig = config.config
    )
  }

  multiGoldenTest("Parse dialect with library", "dialect.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        directory = s"$basePath/dialect-library",
        amlConfig = config.config
    )
  }

  multiGoldenTest("Parse empty but present type discriminator field", "empty-type-discriminator-field.%s") { config =>
    cycle(
        "empty-type-discriminator-field.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        amlConfig = config.config
    )
  }

  multiGoldenTest("Parse empty value in type discriminator", "empty-type-discriminator-value.%s") { config =>
    cycle(
        "empty-type-discriminator-value.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        amlConfig = config.config
    )
  }

  multiGoldenTest("Parse annotation mappings & semantic extensions", "dialect.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        directory = s"$basePath/annotation-mappings",
        amlConfig = config.config
    )
  }

  multiGoldenTest("Parse inexistent annotation mapping reference from semantic extensions", "dialect.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        directory = s"$basePath/annotation-mappings-inexistent-ref",
        amlConfig = config.config
    )
  }

  multiGoldenTest("Parse annotation mappings with extra facets", "dialect.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions.withCompactUris),
        directory = s"$basePath/annotation-mappings-with-extra-facets"
    )
  }

  // JSON

  test("Parse simple JSON dialect") {
    cycle(
        "dialect.json",
        "dialect.jsonld",
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(RenderOptions().withCompactUris.withPrettyPrint),
        directory = s"$basePath/json/simple"
    )
  }

  test("Parse JSON dialect with library") {
    cycle(
        "dialect.json",
        "dialect.jsonld",
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(RenderOptions().withCompactUris.withPrettyPrint),
        directory = s"$basePath/json/with-library"
    )
  }

  test("Parse JSON dialect with vocabulary") {
    cycle(
        "dialect.json",
        "dialect.jsonld",
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(RenderOptions().withCompactUris.withPrettyPrint),
        directory = s"$basePath/json/with-vocabulary"
    )
  }

  test("Parse dialect with $id directive") {
    cycle(
        "dialect.yaml",
        "dialect.jsonld",
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(RenderOptions().withPrettyPrint.withCompactUris),
        directory = s"$basePath/id-directive"
    )
  }

  test("Parse dialect library with $id directive") {
    cycle(
        "dialect.yaml",
        "dialect.jsonld",
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(RenderOptions().withPrettyPrint.withCompactUris),
        directory = s"$basePath/id-directive-library"
    )
  }

  test("Parse dialect fragment with $id directive") {
    cycle(
        "dialect.yaml",
        "dialect.jsonld",
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(RenderOptions().withPrettyPrint.withCompactUris),
        directory = s"$basePath/id-directive-fragment"
    )
  }

  multiGoldenTest("Empty annotation mapping with name has lexical", "dialect.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions.withCompactUris),
        directory = s"$basePath/empty-annotation-mapping"
    )
  }

  multiGoldenTest("Empty semantic extension with name has lexical and SemEx array is Virtual", "dialect.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions.withCompactUris),
        directory = s"$basePath/empty-semantic-extensions"
    )
  }

  multiGoldenTest("Annotation mapping with multiple domains", "dialect.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions.withCompactUris),
        directory = s"$basePath/annotation-mapping-with-multiple-domains"
    )
  }

  multiGoldenTest("Default facet for property mappings", "dialect.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions.withCompactUris),
        directory = s"$basePath/default-facet"
    )
  }

  multiSourceTest("Generate default facet for property mappings", "dialect.%s") { config =>
    cycle(
        config.source,
        "dialect.cycled.yaml",
        mediaType = Some(Mimes.`application/yaml`),
        directory = s"$basePath/default-facet"
    )
  }

  // TODO cycle from JSON-LD to YAML is affected by W-10890167
  multiCycleTest("Conditional facet", s"$basePath/conditional")

  multiCycleTest("Additional properties facet", s"$basePath/additional-properties")

  multiGoldenTest("Parse enum values with different types of values", "dialect.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        Some(Mimes.`application/ld+json`),
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions.withCompactUris),
        directory = s"$basePath/enum-scalars"
    )
  }

  multiSourceTest("Parse enum values with different types of values from JSON-LD to YAML", "dialect.%s") { config =>
    cycle(
        config.source,
        "dialect.cycled.yaml",
        mediaType = Some(Mimes.`application/yaml`),
        directory = s"$basePath/enum-scalars"
    )
  }

  // TODO cycle from JSON-LD to YAML is affected by W-10890167
  multiCycleTest("AllOf facet", s"$basePath/allOf")

  // TODO cycle from JSON-LD to YAML is affected by W-10890167
  multiCycleTest("OneOf facet", s"$basePath/oneOf")

  // TODO cycle from JSON-LD to YAML is affected by W-10890167
  multiCycleTest("Components facet", s"$basePath/components")

  // TODO cycle from JSON-LD to YAML is affected by W-10890167
  multiCycleTest("AllOf complex", s"$basePath/allOf-complex")

  // TODO cycle from JSON-LD to YAML is affected by W-10890167
  multiCycleTest("AllOf nested", s"$basePath/allOf-nested")

  // TODO cycle from JSON-LD to YAML is affected by W-10890167
  multiCycleTest("AllOf nested with allOf", s"$basePath/allOf-nested-allOf")

  // TODO cycle from JSON-LD to YAML is affected by W-10890167
  multiCycleTest("Extended mapping 1", s"$basePath/extended-mapping-1")

  // TODO cycle from JSON-LD to YAML is affected by W-10890167
  multiCycleTest("Extended mapping 2", s"$basePath/extended-mapping-2")

  // TODO cycle from JSON-LD to YAML is affected by W-10890167
  multiCycleTest("Extended mapping 3", s"$basePath/extended-mapping-3")

  multiCycleTest("MinItems facet", s"$basePath/min-items")

  multiCycleTest("Long datatype", s"$basePath/long-datatype")

}
