package amf.dialects
import amf.core.emitter.RenderOptions
import amf.core.remote.{Amf, Aml, Vendor, VocabularyYamlHint}

import scala.concurrent.ExecutionContext

class DialectInstanceRenderTest extends DialectTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override val basePath: String                            = "shared/src/test/resources/vocabularies2/rendering"
  val instances                                            = "shared/src/test/resources/vocabularies2/instances/"

  test("Simple instance rendering") {
    withDialect(
      "dialect.yaml",
      "instance.yaml",
      "instance-golden.yaml",
      VocabularyYamlHint,
      target = Vendor.AML,
      renderOptions = Some(RenderOptions().withNodeIds),
      directory = s"$basePath/simple-dialect"
    )
  }

  test("Simple nested instance rendering") {
    withDialect("dialect.yaml",
                "instance.yaml",
                "instance-golden.yaml",
                VocabularyYamlHint,
                target = Vendor.AML,
                directory = s"$basePath/simple-nesting")
  }

  test("Simple node union rendering") {
    withDialect("dialect.yaml",
                "instance.yaml",
                "instance-golden.yaml",
                VocabularyYamlHint,
                target = Vendor.AML,
                directory = s"$basePath/simple-node-union")
  }

  test("render 1 (AMF) test") {
    withDialect("dialect1.raml",
                "example1.raml",
                "example1.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 1b (AMF) test") {
    withDialect("dialect1.raml",
                "example1b.raml",
                "example1b.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  // TODO un-ignore after fixing APIMF-2326
  ignore("render 1 with annotations test") {
    withDialect("dialect1.raml",
                "example1_annotations.yaml",
                "example1_annotations.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 2 test") {
    withDialect("dialect2.raml",
                "example2.raml",
                "example2.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 3 test") {
    withDialect("dialect3.raml",
                "example3.raml",
                "example3.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 4 test") {
    withDialect("dialect4.raml",
                "example4.raml",
                "example4.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 5 test") {
    withDialect("dialect5.raml",
                "example5.raml",
                "example5.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 6 test") {
    withDialect("dialect6.raml",
                "example6.raml",
                "example6.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 6b $ref test") {
    withDialect("dialect6.raml",
                "example6b.raml",
                "example6b.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 7 test") {
    withDialect("dialect7.raml",
                "example7.raml",
                "example7.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 8 test") {
    withDialect("dialect8.raml",
                "example8.raml",
                "example8.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 8b $include test") {
    withDialect("dialect8.raml",
                "example8b.yaml",
                "example8b.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 8c $ref test") {
    withDialect("dialect8.raml",
                "example8c.yaml",
                "example8c.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 8 (fragment) test") {
    withDialect("dialect8.raml",
                "fragment8.raml",
                "fragment8.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 9 test") {
    withDialect("dialect9.raml",
                "example9.raml",
                "example9.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  // TODO un-ignore after fixing APIMF-2328
  ignore("render 9b $ref test") {
    withDialect("dialect9.raml",
                "example9b.raml",
                "example9b.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 10a test") {
    withDialect("dialect10.raml",
                "example10a.raml",
                "example10a.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 10b test") {
    withDialect("dialect10.raml",
                "example10b.raml",
                "example10b.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 10c test") {
    withDialect("dialect10.raml",
                "example10c.raml",
                "example10c.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 11 test") {
    withDialect("dialect11.raml",
                "example11.raml",
                "example11.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 13a (test union inline)") {
    withDialect("dialect13a.raml",
                "example13a.raml",
                "example13a.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 13b (test union)") {
    withDialect("dialect13b.raml",
                "example13b.raml",
                "example13b.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 13c (test union with extends)") {
    withDialect("dialect13c.raml",
                "example13c.raml",
                "example13c.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 14 test") {
    withDialect("dialect14.raml",
                "example14.raml",
                "example14.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 15 test") {
    withDialect("dialect15a.raml",
                "example15.raml",
                "example15.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 16 test") {
    withDialect("dialect16a.raml",
                "example16a.raml",
                "example16a.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 16 $include test") {
    withDialect("dialect16a.raml",
                "example16c.yaml",
                "example16c.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 18 test") {
    withDialect("dialect18.raml",
                "example18.raml",
                "example18.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 18 b test") {
    withDialect("dialect18.raml",
                "example18b.raml",
                "example18b.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 19 test") {
    withDialect("dialect19.raml",
                "example19.raml",
                "example19.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 20 test") {
    withDialect("dialect20.raml",
                "example20.raml",
                "example20.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 23 test") {
    withDialect("dialect23.raml",
                "example23.raml",
                "example23.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 24 test") {
    withDialect("dialect24.raml",
                "example24.raml",
                "example24.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 24b test") {
    withDialect("dialect24.raml",
                "example24b.raml",
                "example24b.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 24c test") {
    withDialect("dialect24.raml",
                "example24c.raml",
                "example24c.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 27a test") {
    withDialect("dialect27.raml",
                "example27a.raml",
                "example27a.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 28 test") {
    withDialect("dialect28.raml",
                "example28.raml",
                "example28.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 30 test") {
    withDialect("dialect30.raml",
                "example30.raml",
                "example30.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 31 test") {
    withDialect("dialect31.raml",
                "example31.raml",
                "example31.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 29 test - keyproperty") {
    withDialect("dialect29.raml",
                "example29.raml",
                "example29.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 29 invalid test - keyproperty") {
    withDialect("dialect29.raml",
                "example29.raml",
                "example29.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  // Known difference JVM - JS (JVM emits 4.0, JS emits 4)
  ignore("parse 32 test") {
    withDialect("dialect32.raml",
                "example32.raml",
                "example32.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

}
