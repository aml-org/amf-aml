package amf.testing.render

import amf.aml.client.scala.AMLConfiguration
import amf.aml.internal.transform.pipelines.DefaultAMLTransformationPipeline
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.Mimes.`application/ld+json`
import amf.core.internal.remote.{Aml, Mimes, Spec, VocabularyYamlHint}
import amf.testing.common.utils.DialectTests

import scala.concurrent.ExecutionContext

class DialectInstanceRenderTest extends DialectTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override val basePath: String                            = "amf-aml/shared/src/test/resources/vocabularies2/rendering"
  val instances = "amf-aml/shared/src/test/resources/vocabularies2/instances/"

  test("Simple instance rendering") {
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      "instance-golden.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      renderOptions = Some(RenderOptions().withNodeIds),
      directory = s"$basePath/simple-dialect"
    )
  }

  test("Simple nested instance rendering") {
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      "instance-golden.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = s"$basePath/simple-nesting"
    )
  }

  test("Simple node union rendering") {
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      "instance-golden.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = s"$basePath/simple-node-union"
    )
  }

  test("render 1 (AMF) test") {
    cycleWithDialect(
      "dialect1.yaml",
      "example1.yaml",
      "example1.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 1b (AMF) test") {
    cycleWithDialect(
      "dialect1.yaml",
      "example1b.yaml",
      "example1b.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 1 with annotations test") {
    cycleWithDialect(
      "dialect1.yaml",
      "example1_annotations.yaml",
      "example1_annotations.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 2 test") {
    cycleWithDialect(
      "dialect2.yaml",
      "example2.yaml",
      "example2.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 3 test") {
    cycleWithDialect(
      "dialect3.yaml",
      "example3.yaml",
      "example3.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 4 test") {
    cycleWithDialect(
      "dialect4.yaml",
      "example4.yaml",
      "example4.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 5 test") {
    cycleWithDialect(
      "dialect5.yaml",
      "example5.yaml",
      "example5.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 6 test") {
    cycleWithDialect(
      "dialect6.yaml",
      "example6.yaml",
      "example6.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 6b $ref test") {
    cycleWithDialect(
      "dialect6.yaml",
      "example6b.yaml",
      "example6b.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 7 test") {
    cycleWithDialect(
      "dialect7.yaml",
      "example7.yaml",
      "example7.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 8 test") {
    cycleWithDialect(
      "dialect8.yaml",
      "example8.yaml",
      "example8.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 8b $include test") {
    cycleWithDialect(
      "dialect8.yaml",
      "example8b.yaml",
      "example8b.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 8c $ref test") {
    cycleWithDialect(
      "dialect8.yaml",
      "example8c.yaml",
      "example8c.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 8 (fragment) test") {
    cycleWithDialect(
      "dialect8.yaml",
      "fragment8.yaml",
      "fragment8.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 9 test") {
    cycleWithDialect(
      "dialect9.yaml",
      "example9.yaml",
      "example9.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 9b $ref test") {
    cycleWithDialect(
      "dialect9.yaml",
      "example9b.yaml",
      "example9b-golden.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 10a test") {
    cycleWithDialect(
      "dialect10.yaml",
      "example10a.yaml",
      "example10a.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 10b test") {
    cycleWithDialect(
      "dialect10.yaml",
      "example10b.yaml",
      "example10b.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 10c test") {
    cycleWithDialect(
      "dialect10.yaml",
      "example10c.yaml",
      "example10c.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 11 test") {
    cycleWithDialect(
      "dialect11.yaml",
      "example11.yaml",
      "example11.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 13a (test union inline)") {
    cycleWithDialect(
      "dialect13a.yaml",
      "example13a.yaml",
      "example13a.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 13b (test union)") {
    cycleWithDialect(
      "dialect13b.yaml",
      "example13b.yaml",
      "example13b.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 13c (test union with extends)") {
    cycleWithDialect(
      "dialect13c.yaml",
      "example13c.yaml",
      "example13c.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 14 test") {
    cycleWithDialect(
      "dialect14.yaml",
      "example14.yaml",
      "example14.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 15 test") {
    for {
      config <- AMLConfiguration.predefined().withDialect(s"file://$instances/dialect15b.yaml")
      assertion <- cycleWithDialect(
        "dialect15a.yaml",
        "example15.yaml",
        "example15.yaml",
        mediaType = Some(Mimes.`application/yaml`),
        directory = instances,
        baseConfig = config
      )
    } yield {
      assertion
    }

  }

  test("render 16 test") {
    for {
      config <- AMLConfiguration.predefined().withDialect(s"file://$instances/dialect16b.yaml")
      assertion <- cycleWithDialect(
        "dialect16a.yaml",
        "example16a.yaml",
        "example16a.yaml",
        mediaType = Some(Mimes.`application/yaml`),
        directory = instances,
        baseConfig = config
      )
    } yield {
      assertion
    }
  }

  test("render 16 $include test") {
    for {
      config <- AMLConfiguration.predefined().withDialect(s"file://$instances/dialect16b.yaml")
      assertion <- cycleWithDialect(
        "dialect16a.yaml",
        "example16c.yaml",
        "example16c.yaml",
        mediaType = Some(Mimes.`application/yaml`),
        directory = instances,
        baseConfig = config
      )
    } yield {
      assertion
    }
  }

  test("render 18 test") {
    cycleWithDialect(
      "dialect18.yaml",
      "example18.yaml",
      "example18.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 18 b test") {
    cycleWithDialect(
      "dialect18.yaml",
      "example18b.yaml",
      "example18b.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 19 test") {
    cycleWithDialect(
      "dialect19.yaml",
      "example19.yaml",
      "example19.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 20 test") {
    cycleWithDialect(
      "dialect20.yaml",
      "example20.yaml",
      "example20.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 23 test") {
    cycleWithDialect(
      "dialect23.yaml",
      "example23.yaml",
      "example23.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 24 test") {
    cycleWithDialect(
      "dialect24.yaml",
      "example24.yaml",
      "example24.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 24b test") {
    cycleWithDialect(
      "dialect24.yaml",
      "example24b.yaml",
      "example24b.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 24c test") {
    cycleWithDialect(
      "dialect24.yaml",
      "example24c.yaml",
      "example24c.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 27a test") {
    cycleWithDialect(
      "dialect27.yaml",
      "example27a.yaml",
      "example27a.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 28 test") {
    cycleWithDialect(
      "dialect28.yaml",
      "example28.yaml",
      "example28.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 30 test") {
    cycleWithDialect(
      "dialect30.yaml",
      "example30.yaml",
      "example30.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 31 test") {
    cycleWithDialect(
      "dialect31.yaml",
      "example31.yaml",
      "example31.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 29 test - keyproperty") {
    cycleWithDialect(
      "dialect29.yaml",
      "example29.yaml",
      "example29.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("render 29 invalid test - keyproperty") {
    cycleWithDialect(
      "dialect29.yaml",
      "example29.yaml",
      "example29.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  // Known difference JVM - JS (JVM emits 4.0, JS emits 4)
  ignore("parse 32 test") {
    cycleWithDialect(
      "dialect32.yaml",
      "example32.yaml",
      "example32.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = instances
    )
  }

  test("Big double without scientific notation") {
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      "instance-golden.yaml",
      mediaType = Some(Mimes.`application/yaml`),
      directory = s"$basePath/double-scientific-notation"
    )
  }

}

class ResolvedDialectInstancesRenderTest extends DialectTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override val basePath: String                            = "amf-aml/shared/src/test/resources/vocabularies2/rendering"
  val instances = "amf-aml/shared/src/test/resources/vocabularies2/instances/"

  test("Flatten multiple documents YAML") {
    cycleWithDialect(
      "dialect.yaml",
      "dog.yaml",
      "dog.flattened.yaml",
      mediaType = None,
      directory = s"$instances/many-documents/"
    )
  }

  test("Flatten multiple documents JSON-LD") {
    cycleWithDialect(
      "dialect.yaml",
      "dog.yaml",
      "dog.flattened.jsonld",
      mediaType = Some(`application/ld+json`),
      directory = s"$instances/many-documents/",
      renderOptions = Some(RenderOptions().withFlattenedJsonLd.withPrettyPrint.withCompactUris)
    )
  }

  override def transform(unit: BaseUnit, amlConfig: AMLConfiguration): BaseUnit = {
    amlConfig.baseUnitClient().transform(unit, DefaultAMLTransformationPipeline.name).baseUnit
  }
}
