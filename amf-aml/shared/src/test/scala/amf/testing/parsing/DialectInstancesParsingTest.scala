package amf.testing.parsing

import amf.aml.client.scala.AMLConfiguration
import amf.aml.internal.annotations.FromUnionRangeMapping
import amf.core.client.scala.config.RenderOptions
import amf.core.internal.plugins.document.graph.{EmbeddedForm, FlattenedForm}
import amf.core.internal.remote._
import amf.testing.common.utils.DialectTests
import org.scalatest.Assertion

import scala.concurrent.Future

trait DialectInstancesParsingTest extends DialectTests {

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/instances/"

  if (platform.name == "jvm") {
    ignore("parse 1b test") {
      cycleWithDialect(
        "dialect1.yaml",
        "example1b.yaml",
        "example1b.json",
        Some(Mimes.`application/ld+json`),
        renderOptions = Some(RenderOptions().withoutAmfJsonLdSerialization)
      )
    }
  }

  multiGoldenTest("parse 1 (AMF) test", "example1.amf.%s") { config =>
    cycleWithDialect(
      "dialect1.yaml",
      "example1.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 1b (AMF) test", "example1b.amf.%s") { config =>
    cycleWithDialect(
      "dialect1.yaml",
      "example1b.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 1 with annotations test", "example1_annotations.%s") { config =>
    cycleWithDialect(
      "dialect1.yaml",
      "example1_annotations.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 2 test", "example2.%s") { config =>
    cycleWithDialect(
      "dialect2.yaml",
      "example2.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 3 test", "example3.%s") { config =>
    cycleWithDialect(
      "dialect3.yaml",
      "example3.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 4 test", "example4.%s") { config =>
    cycleWithDialect(
      "dialect4.yaml",
      "example4.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 5 test", "example5.%s") { config =>
    cycleWithDialect(
      "dialect5.yaml",
      "example5.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 6 test", "example6.%s") { config =>
    cycleWithDialect(
      "dialect6.yaml",
      "example6.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 6b $ref test", "example6b.%s") { config =>
    cycleWithDialect(
      "dialect6.yaml",
      "example6b.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 7 test", "example7.%s") { config =>
    cycleWithDialect(
      "dialect7.yaml",
      "example7.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 8 test", "example8.%s") { config =>
    cycleWithDialect(
      "dialect8.yaml",
      "example8.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 8b $include test", "example8b.%s") { config =>
    cycleWithDialect(
      "dialect8.yaml",
      "example8b.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 8c $ref test", "example8c.%s") { config =>
    cycleWithDialect(
      "dialect8.yaml",
      "example8c.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 8 (fragment) test", "fragment8.%s") { config =>
    cycleWithDialect(
      "dialect8.yaml",
      "fragment8.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 9 test", "example9.%s") { config =>
    cycleWithDialect(
      "dialect9.yaml",
      "example9.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 9b $ref test", "example9b.%s") { config =>
    cycleWithDialect(
      "dialect9.yaml",
      "example9b.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 10a test", "example10a.%s") { config =>
    cycleWithDialect(
      "dialect10.yaml",
      "example10a.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 10b test", "example10b.%s") { config =>
    cycleWithDialect(
      "dialect10.yaml",
      "example10b.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 10c test", "example10c.%s") { config =>
    cycleWithDialect(
      "dialect10.yaml",
      "example10c.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 11 test", "example11.%s") { config =>
    cycleWithDialect(
      "dialect11.yaml",
      "example11.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 12 test", "example12.%s") { config =>
    for {
      amlConfig <- AMLConfiguration.predefined().withDialect(s"file://$basePath/dialect12.yaml")
      assertion <- withInlineDialect(
        "example12.yaml",
        config.golden,
        mediaType = Some(Mimes.`application/ld+json`),
        amlConfig.withRenderOptions(config.renderOptions)
      )
    } yield {
      assertion
    }
  }

  multiGoldenTest("parse 13a (test union inline)", "example13a.%s") { config =>
    cycleWithDialect(
      "dialect13a.yaml",
      "example13a.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 13b (test union)", "example13b.%s") { config =>
    cycleWithDialect(
      "dialect13b.yaml",
      "example13b.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 13c (test union with extends)", "example13c.%s") { config =>
    cycleWithDialect(
      "dialect13c.yaml",
      "example13c.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 14 test", "example14.%s") { config =>
    cycleWithDialect(
      "dialect14.yaml",
      "example14.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 15 test", "example15.%s") { config =>
    val amlConfig = AMLConfiguration.predefined()
    for {
      nextAmlConfig <- amlConfig.withDialect(
        "file://amf-aml/shared/src/test/resources/vocabularies2/instances/dialect15b.yaml"
      )
      assertion <- cycleWithDialect(
        "dialect15a.yaml",
        "example15.yaml",
        config.golden,
        renderOptions = Some(config.renderOptions),
        mediaType = Some(Mimes.`application/ld+json`),
        baseConfig = nextAmlConfig
      )
    } yield {
      assertion
    }
  }

  multiGoldenTest("parse 16 test", "example16a.%s") { config =>
    val amlConfig = AMLConfiguration.predefined()
    for {
      nextAmlConfig <- amlConfig.withDialect(
        "file://amf-aml/shared/src/test/resources/vocabularies2/instances/dialect16b.yaml"
      )
      assertion <- cycleWithDialect(
        "dialect16a.yaml",
        "example16a.yaml",
        config.golden,
        renderOptions = Some(config.renderOptions),
        mediaType = Some(Mimes.`application/ld+json`),
        baseConfig = nextAmlConfig
      )
    } yield {
      assertion
    }
  }

  multiGoldenTest("parse 16 $include test", "example16c.%s") { config =>
    val amlConfig = AMLConfiguration.predefined()
    for {
      nextAmlConfig <- amlConfig.withDialect(
        "file://amf-aml/shared/src/test/resources/vocabularies2/instances/dialect16b.yaml"
      )
      assertion <- cycleWithDialect(
        "dialect16a.yaml",
        "example16c.yaml",
        config.golden,
        renderOptions = Some(config.renderOptions),
        mediaType = Some(Mimes.`application/ld+json`),
        baseConfig = nextAmlConfig
      )
    } yield {
      assertion
    }
  }

  multiGoldenTest("parse 17 test", "example17.output.%s") { config =>
    cycleWithDialect(
      "dialect17.input.json",
      "example17.input.json",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 18 test", "example18.%s") { config =>
    cycleWithDialect(
      "dialect18.yaml",
      "example18.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 18 b test", "example18b.%s") { config =>
    cycleWithDialect(
      "dialect18.yaml",
      "example18b.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 19 test", "example19.%s") { config =>
    cycleWithDialect(
      "dialect19.yaml",
      "example19.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 20 test", "example20.%s") { config =>
    cycleWithDialect(
      "dialect20.yaml",
      "example20.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 21a test", "patch21.%s") { config =>
    cycleWithDialect(
      "dialect21.yaml",
      "patch21.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 21b test", "patch21b.%s") { config =>
    cycleWithDialect(
      "dialect21.yaml",
      "patch21b.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 23 test", "example23.%s") { config =>
    cycleWithDialect(
      "dialect23.yaml",
      "example23.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  if (platform.name == "jvm") {
    ignore("parse 23 (non-amf) test") {
      cycleWithDialect(
        "dialect23.yaml",
        "example23.yaml",
        "example23.rdf.json",
        Some(Mimes.`application/ld+json`),
        renderOptions = Some(RenderOptions().withoutAmfJsonLdSerialization)
      )
    }
  }

  multiGoldenTest("parse 24 test", "example24.%s") { config =>
    cycleWithDialect(
      "dialect24.yaml",
      "example24.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 24b test", "example24b.%s") { config =>
    cycleWithDialect(
      "dialect24.yaml",
      "example24b.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 24c test", "example24c.%s") { config =>
    cycleWithDialect(
      "dialect24.yaml",
      "example24c.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 27a test", "example27a.%s") { config =>
    cycleWithDialect(
      "dialect27.yaml",
      "example27a.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 28 test", "example28.%s") { config =>
    cycleWithDialect(
      "dialect28.yaml",
      "example28.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 30 test", "example30.%s") { config =>
    cycleWithDialect(
      "dialect30.yaml",
      "example30.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 31 test", "example31.%s") { config =>
    cycleWithDialect(
      "dialect31.yaml",
      "example31.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 32 test", "example32.%s") { config =>
    cycleWithDialect(
      "dialect32.yaml",
      "example32.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("parse 33 test (Union with no type discriminator)", "example33.%s") { config =>
    cycleWithDialect(
      "dialect33.yaml",
      "example33.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  if (platform.name == "jvm") {
    ignore("generate 1 test") {
      cycleWithDialect(
        "dialect1.yaml",
        "example1.json",
        "example1.yaml",
        None,
        renderOptions = Some(RenderOptions().withoutAmfJsonLdSerialization)
      )
    }
  }

  if (platform.name == "jvm") {
    ignore("generate 23 (non-amf) test") {
      cycleWithDialect(
        "dialect23.yaml",
        "example23.rdf.json",
        "example23.yaml",
        None,
        renderOptions = Some(RenderOptions().withoutAmfJsonLdSerialization)
      )
    }
  }

  ignore("generate 1b test") {
    cycleWithDialect(
      "dialect1.yaml",
      "example1b.json",
      "example1b.yaml",
      None,
      renderOptions = Some(RenderOptions().withoutAmfJsonLdSerialization)
    )
  }

  multiSourceTest("generate 1 (AMF) test", "example1.amf.%s") { config =>
    cycleWithDialect("dialect1.yaml", config.source, "example1.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 1b (AMF) test", "example1b.amf.%s") { config =>
    cycleWithDialect("dialect1.yaml", config.source, "example1b.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 2 test", "example2.%s") { config =>
    cycleWithDialect("dialect2.yaml", config.source, "example2.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 3 test", "example3.%s") { config =>
    cycleWithDialect("dialect3.yaml", config.source, "example3.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 4 test", "example4.%s") { config =>
    cycleWithDialect("dialect4.yaml", config.source, "example4.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 5 test", "example5.%s") { config =>
    cycleWithDialect("dialect5.yaml", config.source, "example5.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 6 test", "example6.%s") { config =>
    cycleWithDialect("dialect6.yaml", config.source, "example6.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 6b $ref test", "example6b.%s") { config =>
    cycleWithDialect("dialect6.yaml", config.source, "example6b.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 7 test", "example7.%s") { config =>
    cycleWithDialect("dialect7.yaml", config.source, "example7.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 8 test", "example8.%s") { config =>
    cycleWithDialect("dialect8.yaml", config.source, "example8.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 8b $include test", "example8b.%s") { config =>
    cycleWithDialect("dialect8.yaml", config.source, "example8b.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 8c $ref test", "example8c.%s") { config =>
    cycleWithDialect("dialect8.yaml", config.source, "example8c.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 8 (fragment) test", "fragment8.%s") { config =>
    cycleWithDialect("dialect8.yaml", config.source, "fragment8.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 9 test", "example9.%s") { config =>
    cycleWithDialect("dialect9.yaml", config.source, "example9.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 9b $ref test", "example9b.%s") { config =>
    cycleWithDialect("dialect9.yaml", config.source, "example9b.json.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 10a test", "example10a.%s") { config =>
    cycleWithDialect("dialect10.yaml", config.source, "example10a.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 10b test", "example10b.%s") { config =>
    cycleWithDialect("dialect10.yaml", config.source, "example10b.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 10c test", "example10c.%s") { config =>
    cycleWithDialect("dialect10.yaml", config.source, "example10c.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 11 test", "example11.%s") { config =>
    cycleWithDialect("dialect11.yaml", config.source, "example11.yaml", Some(Mimes.`application/yaml`))
  }

  ignore("generate 13a test") {
    cycleWithDialect("dialect13a.yaml", "example13a.json", "example13a.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 13b test", "example13b.%s") { config =>
    cycleWithDialect("dialect13b.yaml", config.source, "example13b.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 13c test", "example13c.%s") { config =>
    cycleWithDialect("dialect13c.yaml", config.source, "example13c.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 14 test", "example14.%s") { config =>
    cycleWithDialect("dialect14.yaml", config.source, "example14.yaml", Some(Mimes.`application/yaml`))
  }

  // TODO ARM for instance needed
//  multiSourceTest("generate 15 test", "example15.%s") { config =>
//    for {
//      _         <- AMLPlugin.registry.registerDialect(s"file://$basePath/dialect15b.yaml")
//      assertion <- cycleWithDialect("dialect15a.yaml", config.source, "example15.yaml", arget = Aml)
//    } yield {
//      assertion
//    }
//
//  }

//  multiSourceTest("generate 16 test", "example16a.%s") { config =>
//    for {
//      _         <- AMLPlugin.registry.registerDialect(s"file://$basePath/dialect16b.yaml")
//      assertion <- cycleWithDialect("dialect16a.yaml", config.source, "example16a.yaml", arget = Aml)
//    } yield {
//      assertion
//    }
//  }

//  multiSourceTest("generate 16c test", "example16c.%s") { config =>
//    for {
//      _         <- AMLPlugin.registry.registerDialect(s"file://$basePath/dialect16b.yaml")
//      assertion <- cycleWithDialect("dialect16a.yaml", config.source, "example16c.yaml", arget = Aml)
//    } yield {
//      assertion
//    }
//  }

  multiSourceTest("generate 18 test", "example18.%s") { config =>
    cycleWithDialect("dialect18.yaml", config.source, "example18.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 18b test", "example18b.%s") { config =>
    cycleWithDialect("dialect18.yaml", config.source, "example18b.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 19 test", "example19.%s") { config =>
    cycleWithDialect("dialect19.yaml", config.source, "example19.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 23 test", "example23.%s") { config =>
    cycleWithDialect("dialect23.yaml", config.source, "example23.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 24 test", "example24.%s") { config =>
    cycleWithDialect("dialect24.yaml", config.source, "example24.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 24 b test", "example24b.%s") { config =>
    cycleWithDialect("dialect24.yaml", config.source, "example24b.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 24 c test", "example24c.%s") { config =>
    cycleWithDialect("dialect24.yaml", config.source, "example24c.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 27a test", "example27a.%s") { config =>
    cycleWithDialect("dialect27.yaml", config.source, "example27a.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 28 test", "example28.%s") { config =>
    cycleWithDialect("dialect28.yaml", config.source, "example28.yaml", Some(Mimes.`application/yaml`))
  }

  multiGoldenTest("parse 29 test - keyproperty", "example29.%s") { config =>
    cycleWithDialect(
      "dialect29.yaml",
      "example29.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiSourceTest("generate 29 test - keyproperty", "example29.%s") { config =>
    cycleWithDialect("dialect29.yaml", config.source, "example29.yaml", Some(Mimes.`application/yaml`))
  }

  multiGoldenTest("parse 29 invalid test - keyproperty", "example29.%s") { config =>
    cycleWithDialect(
      "dialect29.yaml",
      "example29.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiSourceTest("generate 30 test", "example30.%s") { config =>
    cycleWithDialect("dialect30.yaml", config.source, "example30.yaml", Some(Mimes.`application/yaml`))
  }

  multiSourceTest("generate 31 test", "example31.%s") { config =>
    cycleWithDialect("dialect31.yaml", config.source, "example31.yaml", Some(Mimes.`application/yaml`))
  }

  multiGoldenTest("Generate instance with invalid property terms", "/invalids/schema-uri/instance.%s") { config =>
    cycleWithDialect(
      "/invalids/schema-uri/dialect.yaml",
      "/invalids/schema-uri/instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("Instance with similar fragment names minor", "minor.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "minor.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/colliding-fragments"
    )
  }

  multiGoldenTest("Instance with similar fragment names publicMinor", "publicMinor.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "publicMinor.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/colliding-fragments"
    )
  }

  multiGoldenTest("Parse mapKey and mapValue", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/map-key-value"
    )
  }

  multiGoldenTest("Parse YAML instance with empty node", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/empty-node-yaml"
    )
  }

  multiGoldenTest("Parse JSON instance with empty node", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.json",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/empty-node-json"
    )
  }

  // TODO ARM: forInstance needed
//  multiGoldenTest("Parse instance with $dialect", "instance.%s") { config =>
//    for {
//      _ <- AMLPlugin.registry.registerDialect(
//          "file://amf-aml/shared/src/test/resources/vocabularies2/instances/$dialect/dialectB.yaml")
//      assertion <- cycleWithDialect(
//          "dialect.yaml",
//          "instance.yaml",
//          config.golden,
//          //          mediaType = Some(Mimes.`application/ld+json`,
//          renderOptions = Some(config.renderOptions),
//          directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/$dialect"
//      )
//    } yield {
//      assertion
//    }
//  }

  multiGoldenTest("Parse instance with includes", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/includes"
    )
  }

  multiGoldenTest("Parse instance with $dialect and includes", "instance.%s") { config =>
    for {
      amlConfig <- AMLConfiguration
        .predefined()
        .withDialect(
          "file://amf-aml/shared/src/test/resources/vocabularies2/instances/$dialect-with-includes/dialectB.yaml"
        )
      assertion <- cycleWithDialect(
        "dialect.yaml",
        "instance.yaml",
        config.golden,
        mediaType = Some(Mimes.`application/ld+json`),
        renderOptions = Some(config.renderOptions),
        directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/$dialect-with-includes",
        baseConfig = amlConfig
      )
    } yield {
      assertion
    }
  }

  multiGoldenTest("Parse instance with $id", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/$id"
    )
  }

  multiGoldenTest("Parse instance with id template", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/id-template"
    )
  }

  multiGoldenTest("Parse instance with primary key", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = "amf-aml/shared/src/test/resources/vocabularies2/instances/node-with-primary-key"
    )
  }

  multiGoldenTest("Parse from instance from JSON-LD with extended term definitions in @context", "instance.golden.%s") {
    config =>
      cycleWithDialect(
        "dialect.yaml",
        "instance.source.flattened.jsonld",
        config.golden,
        mediaType = Some(Mimes.`application/ld+json`),
        directory = basePath + "jsonld-extended-term-definitions/",
        renderOptions = Some(config.renderOptions)
      )
  }

  multiGoldenTest("Parse instance with $base facet", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      directory = basePath + "$base/",
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("Parse instance with $base facet and id template", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      directory = basePath + "$base-with-id-template/",
      renderOptions = Some(config.renderOptions)
    )
  }

  test("Clone instance from dialect") {
    withDialect(s"file://$basePath/dialect31.yaml") { (_, config) =>
      for {
        bu <- parse(s"file://$basePath/dialect31.yaml", platform, Some(Aml.mediaType), config)
      } yield {
        val clone = bu.cloneUnit()
        clone.fields.foreach(f => assert(bu.fields.exists(f._1)))
        bu.fields.foreach(f => assert(clone.fields.exists(f._1)))
        assert(bu != clone) // not the SAME object
      }
    }
  }

  multiGoldenTest("Parse instance with simple native link", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/simple-native-links/"
    )
  }

  multiGoldenTest("Parse instance with native links and template ids", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/native-links-with-template-ids/"
    )
  }

  multiGoldenTest("Parse instance with native links and extra properties", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/native-link-with-extra-properties/"
    )
  }

  multiGoldenTest("Parse instance with native links and native targets", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/native-links-with-native-target/"
    )
  }

  multiSourceTest("Generate instance with simple native link", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      config.source,
      "instance.yaml",
      Some(Mimes.`application/yaml`),
      directory = s"$basePath/simple-native-links/"
    )
  }

  multiSourceTest("Generate instance with native links and template ids", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      config.source,
      "instance.yaml",
      Some(Mimes.`application/yaml`),
      directory = s"$basePath/native-links-with-template-ids/"
    )
  }

  multiSourceTest("Generate instance with native links and native targets", "instance.%s") { config =>
    val golden = config.jsonLdDocumentForm match {
      case FlattenedForm => "instance.flattened.yaml"
      case EmbeddedForm  => "instance.expanded.yaml"
      case _             => "instance.flattened.yaml"
    }
    cycleWithDialect(
      "dialect.yaml",
      config.source,
      golden,
      Some(Mimes.`application/yaml`),
      directory = s"$basePath/native-links-with-native-target/"
    )
  }

  multiGoldenTest("Parse instance with compact URIs", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions.withCompactUris),
      directory = s"$basePath/compact-uris/"
    )
  }

  multiGoldenTest("Id produced from idTemplate is encoded", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/encoded-id-template/"
    )
  }

  multiGoldenTest("Simple node mapping extension", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/simple-node-mapping-extension/"
    )
  }

  multiGoldenTest("Node mapping extension with overriden properties", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/node-mapping-extension-with-overriden-properties/"
    )
  }

  multiGoldenTest("Node mapping extension with id templates", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/node-mapping-extension-with-id-templates/"
    )
  }

  multiGoldenTest("Array property mapping with single string element", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.jsonld",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/array-property-mappings-with-single-string-element/"
    )
  }

  multiGoldenTest("Array property mapping with single object element", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.jsonld",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/array-property-mappings-with-single-object-element/"
    )
  }

  multiGoldenTest("mapKey and mapValue without classterm", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/map-key-value-without-classterm/"
    )
  }

  // TODO W-10795527
  ignore("Cyclic references") {
    cycleWithDialect(
      "dialect.yaml",
      "instance.flattened.jsonld",
      "instance.golden.flattened.jsonld",
      Some(Mimes.`application/yaml`),
      renderOptions = Some(RenderOptions().withFlattenedJsonLd.withPrettyPrint),
      directory = s"$basePath/cyclic-references/"
    )
  }

  multiGoldenTest("mapValue with multiple values", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/mapValue-with-multiple/"
    )
  }

  test("different encodes should be named") {
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      "instance.golden.flattened.jsonld",
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(RenderOptions().withFlattenedJsonLd.withPrettyPrint),
      directory = s"$basePath/encoded-named/"
    )
  }

  test("Undefined mapping from UnionRange will contain FromUnionRangeMapping annotation") {
    val amlConfig  = AMLConfiguration.predefined()
    val dialectUri = "file://amf-aml/shared/src/test/resources/vocabularies2/dialects/range-mapping.yaml"

    def declaration(s: String): String = s"$dialectUri#/declarations/$s"

    for {
      nextAmlConfig <- amlConfig.withDialect(dialectUri)
      instance <- nextAmlConfig
        .baseUnitClient()
        .parseDialectInstance(
          "file://amf-aml/shared/src/test/resources/vocabularies2/instances/undefined-union-range-mapping.yaml"
        )
      assertion <- {
        val encoded = instance.dialectInstance.encodes
        val unionPropertyFieldEntry =
          encoded.fields.fields().find(_.field.value.iri() == "http://test.com/v2#unionRangeProp")
        val annotation = unionPropertyFieldEntry.get.value.value.annotations.find(classOf[FromUnionRangeMapping])
        assert(annotation.isDefined)
        val possibleRanges = annotation.get.possibleRanges
        assert(possibleRanges.contains(declaration("A")))
        assert(possibleRanges.contains(declaration("B")))
        assert(possibleRanges.contains(declaration("C")))
      }
    } yield {
      assertion
    }
  }

  test("Defined mapping from UnionRange should not contain FromUnionRangeMapping annotation") {
    val amlConfig  = AMLConfiguration.predefined()
    val dialectUri = "file://amf-aml/shared/src/test/resources/vocabularies2/dialects/range-mapping.yaml"

    def declaration(s: String): String = s"$dialectUri#/declarations/$s"

    for {
      nextAmlConfig <- amlConfig.withDialect(dialectUri)
      instance <- nextAmlConfig
        .baseUnitClient()
        .parseDialectInstance(
          "file://amf-aml/shared/src/test/resources/vocabularies2/instances/defined-union-range-mapping.yaml"
        )
      assertion <- {
        val encoded = instance.dialectInstance.encodes
        val unionPropertyFieldEntry =
          encoded.fields.fields().find(_.field.value.iri() == "http://test.com/v2#unionRangeProp")
        val annotation = unionPropertyFieldEntry.get.value.value.annotations.find(classOf[FromUnionRangeMapping])
        assert(annotation.isEmpty)
      }
    } yield {
      assertion
    }
  }

  test("JSON Instance with Union at root level should omit the $dialect entry") {
    cycleWithDialect(
      "dialect.yaml",
      "instance.json",
      "instance.golden.jsonld",
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(RenderOptions().withPrettyPrint.withCompactUris),
      directory = s"$basePath/json-root-union/"
    )
  }

  test("Instance cycle with mapping ref without term obj") {
    cycleWithDialect(
      "dialect.yaml",
      "instance.json",
      "instance.golden.json",
      mediaType = Some(Mimes.`application/json`),
      renderOptions = Some(RenderOptions().withPrettyPrint.withCompactUris),
      directory = s"$basePath/without-term-obj/"
    )
  }

  test("Instance cycle with mapping ref without term seq") {
    cycleWithDialect(
      "dialect.yaml",
      "instance.json",
      "instance.golden.json",
      mediaType = Some(Mimes.`application/json`),
      renderOptions = Some(RenderOptions().withPrettyPrint.withCompactUris),
      directory = s"$basePath/without-term-seq/"
    )
  }

  multiGoldenTest("if/then/else parsing error should go with else branch", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/if-then-else-parsing-error/"
    )
  }

  multiGoldenTest("if/then/else valid if should go with then branch", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/if-then-else-valid-if/"
    )
  }

  multiGoldenTest("if/then/else invalid if should go with else branch", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/if-then-else-invalid-if/"
    )
  }

  multiSourceTest("if/then/else parsing error should go with else branch from JSON-LD to YAML", "instance.%s") {
    config =>
      cycleWithDialect(
        "dialect.yaml",
        config.source,
        "instance.multi-source.yaml",
        Some(Mimes.`application/yaml`),
        directory = s"$basePath/if-then-else-parsing-error/"
      )
  }

  multiSourceTest("if/then/else valid if should go with then branch from JSON-LD to YAML", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      config.source,
      "instance.multi-source.yaml",
      Some(Mimes.`application/yaml`),
      directory = s"$basePath/if-then-else-valid-if/"
    )
  }

  multiSourceTest("if/then/else invalid if should go with else branch from JSON-LD to YAML", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      config.source,
      "instance.multi-source.yaml",
      Some(Mimes.`application/yaml`),
      directory = s"$basePath/if-then-else-invalid-if/"
    )
  }

  multiGoldenTest("combination: allOf with nested oneOf from YAML to JSON-LD", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/allOf-with-nested-oneOf/"
    )
  }

  multiSourceTest("combination: allOf with nested oneOf from JSON-LD to YAML", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      config.source,
      "instance.cycled.yaml",
      Some(Mimes.`application/yaml`),
      directory = s"$basePath/allOf-with-nested-oneOf/"
    )
  }

  multiGoldenTest("combination: allOf with simple mappings from YAML to JSON-LD", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/allOf-simple/"
    )
  }

  multiSourceTest("combination: allOf with simple mappings from JSON-LD to YAML", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      config.source,
      "instance.cycled.yaml",
      Some(Mimes.`application/yaml`),
      directory = s"$basePath/allOf-simple/"
    )
  }

  multiGoldenTest("combination: allOf with simple mappings and properties from YAML to JSON-LD", "instance.%s") {
    config =>
      cycleWithDialect(
        "dialect.yaml",
        "instance.yaml",
        config.golden,
        mediaType = Some(Mimes.`application/ld+json`),
        renderOptions = Some(config.renderOptions),
        directory = s"$basePath/allOf-properties/"
      )
  }

  multiSourceTest("combination: allOf with simple mappings and properties from JSON-LD to YAML", "instance.%s") {
    config =>
      cycleWithDialect(
        "dialect.yaml",
        config.source,
        "instance.cycled.yaml",
        Some(Mimes.`application/yaml`),
        directory = s"$basePath/allOf-properties/"
      )
  }

  multiGoldenTest("combination: allOf with oneOf and if-then-else from YAML to JSON-LD", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/allOf-oneOf-if-then-else/"
    )
  }

  multiSourceTest("combination: allOf with oneOf and if-then-else JSON-LD to YAML", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      config.source,
      "instance.cycled.yaml",
      Some(Mimes.`application/yaml`),
      directory = s"$basePath/allOf-oneOf-if-then-else/"
    )
  }

  multiGoldenTest("Long datatype YAML to JSON-LD", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      mediaType = Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/long-datatype/"
    )
  }

  multiSourceTest("Long datatype JSON-LD to YAML", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      config.source,
      "instance.cycled.yaml",
      Some(Mimes.`application/yaml`),
      directory = s"$basePath/long-datatype/"
    )
  }

  // noinspection SameParameterValue
  protected def withInlineDialect(
      source: String,
      golden: String,
      mediaType: Option[String],
      amlConfig: AMLConfiguration,
      renderOptions: Option[RenderOptions] = None
  ): Future[Assertion] =
    cycle(source, golden, mediaType, amlConfig = amlConfig)
}
