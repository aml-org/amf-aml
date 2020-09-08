package amf.dialects

import amf.client.parse.DefaultParserErrorHandler
import amf.core.emitter.RenderOptions
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.core.remote._
import amf.plugins.document.vocabularies.AMLPlugin
import org.scalatest.Assertion

import scala.concurrent.{ExecutionContext, Future}

trait DialectInstancesParsingTest extends DialectTests {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "shared/src/test/resources/vocabularies2/instances/"

  if (platform.name == "jvm") {
    ignore("parse 1b test") {
      withDialect("dialect1.yaml",
                  "example1b.yaml",
                  "example1b.json",
                  VocabularyYamlHint,
                  Amf,
                  useAmfJsonldSerialization = false)
    }
  }

  multiGoldenTest("parse 1 (AMF) test", "example1.amf.%s") { config =>
    withDialect("dialect1.yaml",
                "example1.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 1b (AMF) test", "example1b.amf.%s") { config =>
    withDialect("dialect1.yaml",
                "example1b.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 1 with annotations test", "example1_annotations.%s") { config =>
    withDialect("dialect1.yaml",
                "example1_annotations.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 2 test", "example2.%s") { config =>
    withDialect("dialect2.yaml",
                "example2.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 3 test", "example3.%s") { config =>
    withDialect("dialect3.yaml",
                "example3.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 4 test", "example4.%s") { config =>
    withDialect("dialect4.yaml",
                "example4.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 5 test", "example5.%s") { config =>
    withDialect("dialect5.yaml",
                "example5.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 6 test", "example6.%s") { config =>
    withDialect("dialect6.yaml",
                "example6.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 6b $ref test", "example6b.%s") { config =>
    withDialect("dialect6.yaml",
                "example6b.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 7 test", "example7.%s") { config =>
    withDialect("dialect7.yaml",
                "example7.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 8 test", "example8.%s") { config =>
    withDialect("dialect8.yaml",
                "example8.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 8b $include test", "example8b.%s") { config =>
    withDialect("dialect8.yaml",
                "example8b.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 8c $ref test", "example8c.%s") { config =>
    withDialect("dialect8.yaml",
                "example8c.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 8 (fragment) test", "fragment8.%s") { config =>
    withDialect("dialect8.yaml",
                "fragment8.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 9 test", "example9.%s") { config =>
    withDialect("dialect9.yaml",
                "example9.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 9b $ref test", "example9b.%s") { config =>
    withDialect("dialect9.yaml",
                "example9b.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 10a test", "example10a.%s") { config =>
    withDialect("dialect10.yaml",
                "example10a.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 10b test", "example10b.%s") { config =>
    withDialect("dialect10.yaml",
                "example10b.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 10c test", "example10c.%s") { config =>
    withDialect("dialect10.yaml",
                "example10c.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 11 test", "example11.%s") { config =>
    withDialect("dialect11.yaml",
                "example11.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 12 test", "example12.%s") { config =>
    withInlineDialect("example12.yaml",
                      config.golden,
                      VocabularyYamlHint,
                      target = Amf,
                      renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 13a (test union inline)", "example13a.%s") { config =>
    withDialect("dialect13a.yaml",
                "example13a.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 13b (test union)", "example13b.%s") { config =>
    withDialect("dialect13b.yaml",
                "example13b.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 13c (test union with extends)", "example13c.%s") { config =>
    withDialect("dialect13c.yaml",
                "example13c.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 14 test", "example14.%s") { config =>
    withDialect("dialect14.yaml",
                "example14.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 15 test", "example15.%s") { config =>
    withDialect("dialect15a.yaml",
                "example15.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 16 test", "example16a.%s") { config =>
    withDialect("dialect16a.yaml",
                "example16a.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 16 $include test", "example16c.%s") { config =>
    withDialect("dialect16a.yaml",
                "example16c.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 17 test", "example17.output.%s") { config =>
    withDialect("dialect17.input.json",
                "example17.input.json",
                config.golden,
                VocabularyJsonHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 18 test", "example18.%s") { config =>
    withDialect("dialect18.yaml",
                "example18.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 18 b test", "example18b.%s") { config =>
    withDialect("dialect18.yaml",
                "example18b.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 19 test", "example19.%s") { config =>
    withDialect("dialect19.yaml",
                "example19.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 20 test", "example20.%s") { config =>
    withDialect("dialect20.yaml",
                "example20.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 21a test", "patch21.%s") { config =>
    withDialect("dialect21.yaml",
                "patch21.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 21b test", "patch21b.%s") { config =>
    withDialect("dialect21.yaml",
                "patch21b.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 23 test", "example23.%s") { config =>
    withDialect("dialect23.yaml",
                "example23.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  if (platform.name == "jvm") {
    ignore("parse 23 (non-amf) test") {
      withDialect("dialect23.yaml",
                  "example23.yaml",
                  "example23.rdf.json",
                  VocabularyYamlHint,
                  Amf,
                  useAmfJsonldSerialization = false)
    }
  }

  multiGoldenTest("parse 24 test", "example24.%s") { config =>
    withDialect("dialect24.yaml",
                "example24.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 24b test", "example24b.%s") { config =>
    withDialect("dialect24.yaml",
                "example24b.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 24c test", "example24c.%s") { config =>
    withDialect("dialect24.yaml",
                "example24c.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 27a test", "example27a.%s") { config =>
    withDialect("dialect27.yaml",
                "example27a.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 28 test", "example28.%s") { config =>
    withDialect("dialect28.yaml",
                "example28.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 30 test", "example30.%s") { config =>
    withDialect("dialect30.yaml",
                "example30.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 31 test", "example31.%s") { config =>
    withDialect("dialect31.yaml",
                "example31.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 32 test", "example32.%s") { config =>
    withDialect("dialect32.yaml",
                "example32.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  if (platform.name == "jvm") {
    ignore("generate 1 test") {
      withDialect("dialect1.yaml",
                  "example1.json",
                  "example1.yaml",
                  AmfJsonHint,
                  target = Aml,
                  useAmfJsonldSerialization = false)
    }
  }

  if (platform.name == "jvm") {
    ignore("generate 23 (non-amf) test") {
      withDialect("dialect23.yaml",
                  "example23.rdf.json",
                  "example23.yaml",
                  AmfJsonHint,
                  target = Aml,
                  useAmfJsonldSerialization = false)
    }
  }

  ignore("generate 1b test") {
    withDialect("dialect1.yaml",
                "example1b.json",
                "example1b.yaml",
                AmfJsonHint,
                target = Aml,
                useAmfJsonldSerialization = false)
  }

  multiSourceTest("generate 1 (AMF) test", "example1.amf.%s") { config =>
    withDialect("dialect1.yaml", config.source, "example1.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 1b (AMF) test", "example1b.amf.%s") { config =>
    withDialect("dialect1.yaml", config.source, "example1b.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 2 test", "example2.%s") { config =>
    withDialect("dialect2.yaml", config.source, "example2.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 3 test", "example3.%s") { config =>
    withDialect("dialect3.yaml", config.source, "example3.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 4 test", "example4.%s") { config =>
    withDialect("dialect4.yaml", config.source, "example4.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 5 test", "example5.%s") { config =>
    withDialect("dialect5.yaml", config.source, "example5.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 6 test", "example6.%s") { config =>
    withDialect("dialect6.yaml", config.source, "example6.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 6b $ref test", "example6b.%s") { config =>
    withDialect("dialect6.yaml", config.source, "example6b.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 7 test", "example7.%s") { config =>
    withDialect("dialect7.yaml", config.source, "example7.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 8 test", "example8.%s") { config =>
    withDialect("dialect8.yaml", config.source, "example8.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 8b $include test", "example8b.%s") { config =>
    withDialect("dialect8.yaml", config.source, "example8b.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 8c $ref test", "example8c.%s") { config =>
    withDialect("dialect8.yaml", config.source, "example8c.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 8 (fragment) test", "fragment8.%s") { config =>
    withDialect("dialect8.yaml", config.source, "fragment8.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 9 test", "example9.%s") { config =>
    withDialect("dialect9.yaml", config.source, "example9.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 9b $ref test", "example9b.%s") { config =>
    withDialect("dialect9.yaml", config.source, "example9b.json.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 10a test", "example10a.%s") { config =>
    withDialect("dialect10.yaml", config.source, "example10a.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 10b test", "example10b.%s") { config =>
    withDialect("dialect10.yaml", config.source, "example10b.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 10c test", "example10c.%s") { config =>
    withDialect("dialect10.yaml", config.source, "example10c.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 11 test", "example11.%s") { config =>
    withDialect("dialect11.yaml", config.source, "example11.yaml", AmfJsonHint, target = Aml)
  }

  ignore("generate 13a test") {
    withDialect("dialect13a.yaml", "example13a.json", "example13a.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 13b test", "example13b.%s") { config =>
    withDialect("dialect13b.yaml", config.source, "example13b.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 13c test", "example13c.%s") { config =>
    withDialect("dialect13c.yaml", config.source, "example13c.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 14 test", "example14.%s") { config =>
    withDialect("dialect14.yaml", config.source, "example14.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 15 test", "example15.%s") { config =>
    withDialect("dialect15a.yaml", config.source, "example15.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 16 test", "example16a.%s") { config =>
    withDialect("dialect16a.yaml", config.source, "example16a.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 16c test", "example16c.%s") { config =>
    withDialect("dialect16a.yaml", config.source, "example16c.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 18 test", "example18.%s") { config =>
    withDialect("dialect18.yaml", config.source, "example18.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 18b test", "example18b.%s") { config =>
    withDialect("dialect18.yaml", config.source, "example18b.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 19 test", "example19.%s") { config =>
    withDialect("dialect19.yaml", config.source, "example19.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 23 test", "example23.%s") { config =>
    withDialect("dialect23.yaml", config.source, "example23.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 24 test", "example24.%s") { config =>
    withDialect("dialect24.yaml", config.source, "example24.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 24 b test", "example24b.%s") { config =>
    withDialect("dialect24.yaml", config.source, "example24b.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 24 c test", "example24c.%s") { config =>
    withDialect("dialect24.yaml", config.source, "example24c.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 27a test", "example27a.%s") { config =>
    withDialect("dialect27.yaml", config.source, "example27a.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 28 test", "example28.%s") { config =>
    withDialect("dialect28.yaml", config.source, "example28.yaml", AmfJsonHint, target = Aml)
  }

  multiGoldenTest("parse 29 test - keyproperty", "example29.%s") { config =>
    withDialect("dialect29.yaml",
                "example29.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiSourceTest("generate 29 test - keyproperty", "example29.%s") { config =>
    withDialect("dialect29.yaml", config.source, "example29.yaml", AmfJsonHint, target = Aml)
  }

  multiGoldenTest("parse 29 invalid test - keyproperty", "example29.%s") { config =>
    withDialect("dialect29.yaml",
                "example29.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiSourceTest("generate 30 test", "example30.%s") { config =>
    withDialect("dialect30.yaml", config.source, "example30.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 31 test", "example31.%s") { config =>
    withDialect("dialect31.yaml", config.source, "example31.yaml", AmfJsonHint, target = Aml)
  }

  multiGoldenTest("Generate instance with invalid property terms", "/invalids/schema-uri/instance.%s") { config =>
    withDialect(
      "/invalids/schema-uri/dialect.yaml",
      "/invalids/schema-uri/instance.yaml",
      config.golden,
      VocabularyYamlHint,
      target = Amf,
      renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("Instance with similar fragment names minor", "minor.%s") { config =>
    withDialect(
      "dialect.yaml",
      "minor.yaml",
      config.golden,
      VocabularyYamlHint,
      target = Amf,
      renderOptions = Some(config.renderOptions),
      directory = "shared/src/test/resources/vocabularies2/instances/colliding-fragments"
    )
  }

  multiGoldenTest("Instance with similar fragment names publicMinor", "publicMinor.%s") { config =>
    withDialect(
      "dialect.yaml",
      "publicMinor.yaml",
      config.golden,
      VocabularyYamlHint,
      target = Amf,
      renderOptions = Some(config.renderOptions),
      directory = "shared/src/test/resources/vocabularies2/instances/colliding-fragments"
    )
  }

  multiGoldenTest("Parse mapKey and mapValue", "instance.%s") { config =>
    withDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      VocabularyYamlHint,
      target = Amf,
      renderOptions = Some(config.renderOptions),
      directory = "shared/src/test/resources/vocabularies2/instances/map-key-value"
    )
  }

  multiGoldenTest("Parse YAML instance with empty node", "instance.%s") { config =>
    withDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      VocabularyYamlHint,
      target = Amf,
      renderOptions = Some(config.renderOptions),
      directory = "shared/src/test/resources/vocabularies2/instances/empty-node-yaml"
    )
  }

  multiGoldenTest("Parse JSON instance with empty node", "instance.%s") { config =>
    withDialect(
      "dialect.yaml",
      "instance.json",
      config.golden,
      VocabularyJsonHint,
      target = Amf,
      renderOptions = Some(config.renderOptions),
      directory = "shared/src/test/resources/vocabularies2/instances/empty-node-json"
    )
  }


  test("Clone instance from dialect") {
    val context1 = new CompilerContextBuilder(s"file://$basePath/dialect31.yaml",
                                              platform,
                                              DefaultParserErrorHandler.withRun()).build()
    val context2 = new CompilerContextBuilder(s"file://$basePath/example31.yaml",
                                              platform,
                                              DefaultParserErrorHandler.withRun()).build()
    for {
      _  <- init()
      _  <- new AMFCompiler(context1, None, Some(Aml.name)).build()
      bu <- new AMFCompiler(context2, None, Some(Aml.name)).build()
    } yield {
      val clone = bu.cloneUnit()
      clone.fields.foreach(f => assert(bu.fields.exists(f._1)))
      bu.fields.foreach(f => assert(clone.fields.exists(f._1)))
      assert(bu != clone) // not the SAME object
    }
  }

  protected def withInlineDialect(source: String,
                                  golden: String,
                                  hint: Hint,
                                  target: Vendor,
                                  directory: String = basePath,
                                  renderOptions: Option[RenderOptions] = None,
  ): Future[Assertion] = {
    for {
      _   <- init()
      res <- cycle(source, golden, hint, target, renderOptions = renderOptions)
    } yield {
      res
    }
  }
}
