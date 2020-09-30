package amf.dialects
import amf.core.emitter.RenderOptions
import amf.core.remote.{Amf, Aml, Vendor, VocabularyYamlHint}

import scala.concurrent.ExecutionContext

class DialectInstanceRenderTest extends DialectTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override val basePath: String                            = "amf-aml/shared/src/test/resources/vocabularies2/rendering"
  val instances                                            = "amf-aml/shared/src/test/resources/vocabularies2/instances/"

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
    withDialect("dialect1.yaml",
                "example1.yaml",
                "example1.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 1b (AMF) test") {
    withDialect("dialect1.yaml",
                "example1b.yaml",
                "example1b.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 1 with annotations test") {
    withDialect("dialect1.yaml",
                "example1_annotations.yaml",
                "example1_annotations.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 2 test") {
    withDialect("dialect2.yaml",
                "example2.yaml",
                "example2.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 3 test") {
    withDialect("dialect3.yaml",
                "example3.yaml",
                "example3.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 4 test") {
    withDialect("dialect4.yaml",
                "example4.yaml",
                "example4.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 5 test") {
    withDialect("dialect5.yaml",
                "example5.yaml",
                "example5.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 6 test") {
    withDialect("dialect6.yaml",
                "example6.yaml",
                "example6.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 6b $ref test") {
    withDialect("dialect6.yaml",
                "example6b.yaml",
                "example6b.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 7 test") {
    withDialect("dialect7.yaml",
                "example7.yaml",
                "example7.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 8 test") {
    withDialect("dialect8.yaml",
                "example8.yaml",
                "example8.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 8b $include test") {
    withDialect("dialect8.yaml",
                "example8b.yaml",
                "example8b.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 8c $ref test") {
    withDialect("dialect8.yaml",
                "example8c.yaml",
                "example8c.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 8 (fragment) test") {
    withDialect("dialect8.yaml",
                "fragment8.yaml",
                "fragment8.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 9 test") {
    withDialect("dialect9.yaml",
                "example9.yaml",
                "example9.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 9b $ref test") {
    withDialect("dialect9.yaml",
                "example9b.yaml",
                "example9b-golden.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 10a test") {
    withDialect("dialect10.yaml",
                "example10a.yaml",
                "example10a.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 10b test") {
    withDialect("dialect10.yaml",
                "example10b.yaml",
                "example10b.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 10c test") {
    withDialect("dialect10.yaml",
                "example10c.yaml",
                "example10c.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 11 test") {
    withDialect("dialect11.yaml",
                "example11.yaml",
                "example11.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 13a (test union inline)") {
    withDialect("dialect13a.yaml",
                "example13a.yaml",
                "example13a.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 13b (test union)") {
    withDialect("dialect13b.yaml",
                "example13b.yaml",
                "example13b.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 13c (test union with extends)") {
    withDialect("dialect13c.yaml",
                "example13c.yaml",
                "example13c.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 14 test") {
    withDialect("dialect14.yaml",
                "example14.yaml",
                "example14.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 15 test") {
    withDialect("dialect15a.yaml",
                "example15.yaml",
                "example15.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 16 test") {
    withDialect("dialect16a.yaml",
                "example16a.yaml",
                "example16a.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 16 $include test") {
    withDialect("dialect16a.yaml",
                "example16c.yaml",
                "example16c.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 18 test") {
    withDialect("dialect18.yaml",
                "example18.yaml",
                "example18.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 18 b test") {
    withDialect("dialect18.yaml",
                "example18b.yaml",
                "example18b.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 19 test") {
    withDialect("dialect19.yaml",
                "example19.yaml",
                "example19.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 20 test") {
    withDialect("dialect20.yaml",
                "example20.yaml",
                "example20.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 23 test") {
    withDialect("dialect23.yaml",
                "example23.yaml",
                "example23.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 24 test") {
    withDialect("dialect24.yaml",
                "example24.yaml",
                "example24.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 24b test") {
    withDialect("dialect24.yaml",
                "example24b.yaml",
                "example24b.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 24c test") {
    withDialect("dialect24.yaml",
                "example24c.yaml",
                "example24c.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 27a test") {
    withDialect("dialect27.yaml",
                "example27a.yaml",
                "example27a.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 28 test") {
    withDialect("dialect28.yaml",
                "example28.yaml",
                "example28.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 30 test") {
    withDialect("dialect30.yaml",
                "example30.yaml",
                "example30.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 31 test") {
    withDialect("dialect31.yaml",
                "example31.yaml",
                "example31.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 29 test - keyproperty") {
    withDialect("dialect29.yaml",
                "example29.yaml",
                "example29.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  test("render 29 invalid test - keyproperty") {
    withDialect("dialect29.yaml",
                "example29.yaml",
                "example29.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

  // Known difference JVM - JS (JVM emits 4.0, JS emits 4)
  ignore("parse 32 test") {
    withDialect("dialect32.yaml",
                "example32.yaml",
                "example32.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = instances)
  }

}
