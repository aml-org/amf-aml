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
      withDialect("dialect1.raml",
                  "example1b.raml",
                  "example1b.json",
                  VocabularyYamlHint,
                  Amf,
                  useAmfJsonldSerialization = false)
    }
  }

  multiGoldenTest("parse 1 (AMF) test", "example1.amf.%s") { config =>
    withDialect("dialect1.raml",
                "example1.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 1b (AMF) test", "example1b.amf.%s") { config =>
    withDialect("dialect1.raml",
                "example1b.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 1 with annotations test", "example1_annotations.%s") { config =>
    withDialect("dialect1.raml",
                "example1_annotations.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 2 test", "example2.%s") { config =>
    withDialect("dialect2.raml",
                "example2.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 3 test", "example3.%s") { config =>
    withDialect("dialect3.raml",
                "example3.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 4 test", "example4.%s") { config =>
    withDialect("dialect4.raml",
                "example4.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 5 test", "example5.%s") { config =>
    withDialect("dialect5.raml",
                "example5.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 6 test", "example6.%s") { config =>
    withDialect("dialect6.raml",
                "example6.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 6b $ref test", "example6b.%s") { config =>
    withDialect("dialect6.raml",
                "example6b.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 7 test", "example7.%s") { config =>
    withDialect("dialect7.raml",
                "example7.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 8 test", "example8.%s") { config =>
    withDialect("dialect8.raml",
                "example8.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 8b $include test", "example8b.%s") { config =>
    withDialect("dialect8.raml",
                "example8b.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 8c $ref test", "example8c.%s") { config =>
    withDialect("dialect8.raml",
                "example8c.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 8 (fragment) test", "fragment8.%s") { config =>
    withDialect("dialect8.raml",
                "fragment8.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 9 test", "example9.%s") { config =>
    withDialect("dialect9.raml",
                "example9.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 9b $ref test", "example9b.%s") { config =>
    withDialect("dialect9.raml",
                "example9b.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 10a test", "example10a.%s") { config =>
    withDialect("dialect10.raml",
                "example10a.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 10b test", "example10b.%s") { config =>
    withDialect("dialect10.raml",
                "example10b.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 10c test", "example10c.%s") { config =>
    withDialect("dialect10.raml",
                "example10c.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 11 test", "example11.%s") { config =>
    withDialect("dialect11.raml",
                "example11.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 12 test", "example12.%s") { config =>
    withInlineDialect("example12.raml",
                      config.golden,
                      VocabularyYamlHint,
                      target = Amf,
                      renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 13a (test union inline)", "example13a.%s") { config =>
    withDialect("dialect13a.raml",
                "example13a.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 13b (test union)", "example13b.%s") { config =>
    withDialect("dialect13b.raml",
                "example13b.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 13c (test union with extends)", "example13c.%s") { config =>
    withDialect("dialect13c.raml",
                "example13c.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 14 test", "example14.%s") { config =>
    withDialect("dialect14.raml",
                "example14.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 15 test", "example15.%s") { config =>
    withDialect("dialect15a.raml",
                "example15.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 16 test", "example16a.%s") { config =>
    withDialect("dialect16a.raml",
                "example16a.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 16 $include test", "example16c.%s") { config =>
    withDialect("dialect16a.raml",
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
    withDialect("dialect18.raml",
                "example18.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 18 b test", "example18b.%s") { config =>
    withDialect("dialect18.raml",
                "example18b.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 19 test", "example19.%s") { config =>
    withDialect("dialect19.raml",
                "example19.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 20 test", "example20.%s") { config =>
    withDialect("dialect20.raml",
                "example20.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 21a test", "patch21.%s") { config =>
    withDialect("dialect21.raml",
                "patch21.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 21b test", "patch21b.%s") { config =>
    withDialect("dialect21.raml",
                "patch21b.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 23 test", "example23.%s") { config =>
    withDialect("dialect23.raml",
                "example23.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  if (platform.name == "jvm") {
    ignore("parse 23 (non-amf) test") {
      withDialect("dialect23.raml",
                  "example23.raml",
                  "example23.rdf.json",
                  VocabularyYamlHint,
                  Amf,
                  useAmfJsonldSerialization = false)
    }
  }

  multiGoldenTest("parse 24 test", "example24.%s") { config =>
    withDialect("dialect24.raml",
                "example24.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 24b test", "example24b.%s") { config =>
    withDialect("dialect24.raml",
                "example24b.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 24c test", "example24c.%s") { config =>
    withDialect("dialect24.raml",
                "example24c.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 27a test", "example27a.%s") { config =>
    withDialect("dialect27.raml",
                "example27a.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 28 test", "example28.%s") { config =>
    withDialect("dialect28.raml",
                "example28.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 30 test", "example30.%s") { config =>
    withDialect("dialect30.raml",
                "example30.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 31 test", "example31.%s") { config =>
    withDialect("dialect31.raml",
                "example31.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("parse 32 test", "example32.%s") { config =>
    withDialect("dialect32.raml",
                "example32.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  if (platform.name == "jvm") {
    ignore("generate 1 test") {
      withDialect("dialect1.raml",
                  "example1.json",
                  "example1.raml",
                  AmfJsonHint,
                  target = Aml,
                  useAmfJsonldSerialization = false)
    }
  }

  if (platform.name == "jvm") {
    ignore("generate 23 (non-amf) test") {
      withDialect("dialect23.raml",
                  "example23.rdf.json",
                  "example23.raml",
                  AmfJsonHint,
                  target = Aml,
                  useAmfJsonldSerialization = false)
    }
  }

  ignore("generate 1b test") {
    withDialect("dialect1.raml",
                "example1b.json",
                "example1b.raml",
                AmfJsonHint,
                target = Aml,
                useAmfJsonldSerialization = false)
  }

  multiSourceTest("generate 1 (AMF) test", "example1.amf.%s") { config =>
    withDialect("dialect1.raml", config.source, "example1.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 1b (AMF) test", "example1b.amf.%s") { config =>
    withDialect("dialect1.raml", config.source, "example1b.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 2 test", "example2.%s") { config =>
    withDialect("dialect2.raml", config.source, "example2.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 3 test", "example3.%s") { config =>
    withDialect("dialect3.raml", config.source, "example3.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 4 test", "example4.%s") { config =>
    withDialect("dialect4.raml", config.source, "example4.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 5 test", "example5.%s") { config =>
    withDialect("dialect5.raml", config.source, "example5.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 6 test", "example6.%s") { config =>
    withDialect("dialect6.raml", config.source, "example6.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 6b $ref test", "example6b.%s") { config =>
    withDialect("dialect6.raml", config.source, "example6b.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 7 test", "example7.%s") { config =>
    withDialect("dialect7.raml", config.source, "example7.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 8 test", "example8.%s") { config =>
    withDialect("dialect8.raml", config.source, "example8.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 8b $include test", "example8b.%s") { config =>
    withDialect("dialect8.raml", config.source, "example8b.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 8c $ref test", "example8c.%s") { config =>
    withDialect("dialect8.raml", config.source, "example8c.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 8 (fragment) test", "fragment8.%s") { config =>
    withDialect("dialect8.raml", config.source, "fragment8.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 9 test", "example9.%s") { config =>
    withDialect("dialect9.raml", config.source, "example9.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 9b $ref test", "example9b.%s") { config =>
    withDialect("dialect9.raml", config.source, "example9b.json.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 10a test", "example10a.%s") { config =>
    withDialect("dialect10.raml", config.source, "example10a.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 10b test", "example10b.%s") { config =>
    withDialect("dialect10.raml", config.source, "example10b.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 10c test", "example10c.%s") { config =>
    withDialect("dialect10.raml", config.source, "example10c.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 11 test", "example11.%s") { config =>
    withDialect("dialect11.raml", config.source, "example11.raml", AmfJsonHint, target = Aml)
  }

  ignore("generate 13a test") {
    withDialect("dialect13a.raml", "example13a.json", "example13a.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 13b test", "example13b.%s") { config =>
    withDialect("dialect13b.raml", config.source, "example13b.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 13c test", "example13c.%s") { config =>
    withDialect("dialect13c.raml", config.source, "example13c.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 14 test", "example14.%s") { config =>
    withDialect("dialect14.raml", config.source, "example14.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 15 test", "example15.%s") { config =>
    withDialect("dialect15a.raml", config.source, "example15.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 16 test", "example16a.%s") { config =>
    withDialect("dialect16a.raml", config.source, "example16a.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 16c test", "example16c.%s") { config =>
    withDialect("dialect16a.raml", config.source, "example16c.yaml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 18 test", "example18.%s") { config =>
    withDialect("dialect18.raml", config.source, "example18.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 18b test", "example18b.%s") { config =>
    withDialect("dialect18.raml", config.source, "example18b.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 19 test", "example19.%s") { config =>
    withDialect("dialect19.raml", config.source, "example19.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 23 test", "example23.%s") { config =>
    withDialect("dialect23.raml", config.source, "example23.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 24 test", "example24.%s") { config =>
    withDialect("dialect24.raml", config.source, "example24.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 24 b test", "example24b.%s") { config =>
    withDialect("dialect24.raml", config.source, "example24b.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 24 c test", "example24c.%s") { config =>
    withDialect("dialect24.raml", config.source, "example24c.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 27a test", "example27a.%s") { config =>
    withDialect("dialect27.raml", config.source, "example27a.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 28 test", "example28.%s") { config =>
    withDialect("dialect28.raml", config.source, "example28.raml", AmfJsonHint, target = Aml)
  }

  multiGoldenTest("parse 29 test - keyproperty", "example29.%s") { config =>
    withDialect("dialect29.raml",
                "example29.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiSourceTest("generate 29 test - keyproperty", "example29.%s") { config =>
    withDialect("dialect29.raml", config.source, "example29.raml", AmfJsonHint, target = Aml)
  }

  multiGoldenTest("parse 29 invalid test - keyproperty", "example29.%s") { config =>
    withDialect("dialect29.raml",
                "example29.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions))
  }

  multiSourceTest("generate 30 test", "example30.%s") { config =>
    withDialect("dialect30.raml", config.source, "example30.raml", AmfJsonHint, target = Aml)
  }

  multiSourceTest("generate 31 test", "example31.%s") { config =>
    withDialect("dialect31.raml", config.source, "example31.raml", AmfJsonHint, target = Aml)
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

  multiGoldenTest("Parse node with different terms", "instance.%s") { config =>
    withDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      VocabularyYamlHint,
      target = Amf,
      renderOptions = Some(config.renderOptions),
      directory = "shared/src/test/resources/vocabularies2/instances/node-with-different-class-terms"
    )
  }

  test("Clone instance from dialect") {
    val context1 = new CompilerContextBuilder(s"file://$basePath/dialect31.raml",
                                              platform,
                                              DefaultParserErrorHandler.withRun()).build()
    val context2 = new CompilerContextBuilder(s"file://$basePath/example31.raml",
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
