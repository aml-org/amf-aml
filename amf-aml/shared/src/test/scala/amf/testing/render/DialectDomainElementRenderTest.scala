package amf.testing.render

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.remote.{Amf, Hint, VocabularyYamlHint}
import amf.testing.common.utils.DomainElementCycleTests

import scala.concurrent.ExecutionContext

class DialectDomainElementRenderTest extends DomainElementCycleTests {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override val basePath: String                            = "amf-aml/shared/src/test/resources/vocabularies2/instances/"
  override val baseHint: Hint                              = VocabularyYamlHint
  val rendering: String                                    = "amf-aml/shared/src/test/resources/vocabularies2/rendering"

  val encodes: BaseUnit => Option[DomainElement] = {
    case e: EncodesModel =>
      Some(e.encodes)
    case _ => None
  }

  def declares(i: Int): BaseUnit => Option[DomainElement] = {
    case d: DeclaresModel =>
      Some(d.declares.head)
    case _ => None
  }

  test("Simple node union rendering") {
    renderElement("dialect.yaml",
                  "instance.yaml",
                  encodes,
                  "instance-encodes.yaml",
                  directory = s"$rendering/simple-node-union")
  }

  test("render 1 (AMF) test") {
    renderElement("dialect1.yaml", "example1.yaml", encodes, "example1-encodes.yaml")
  }

  test("render 1b (AMF) test") {
    renderElement("dialect1.yaml", "example1b.yaml", encodes, "example1b-encodes.yaml")
  }

  test("render 1 with annotations test") {
    renderElement("dialect1.yaml", "example1_annotations.yaml", encodes, "example1_annotations-encodes.yaml")
  }

  test("render 6b $ref test") {
    renderElement("dialect6.yaml", "example6b.yaml", encodes, "example6b-encodes.yaml")
  }

  test("render 8b $include test") {
    renderElement("dialect8.yaml", "example8b.yaml", encodes, "example8b-encodes.yaml")
  }

  test("render 8c $ref test") {
    renderElement("dialect8.yaml", "example8c.yaml", encodes, "example8c-encodes.yaml")
  }

  test("render 9 test library reference using alias") {
    renderElement("dialect9.yaml", "example9.yaml", declares(0), "example9-encodes.yaml")
  }

  test("render 10a test") {
    renderElement("dialect10.yaml", "example10a.yaml", encodes, "example10a-encodes.yaml")
  }

  test("render 13a (test union inline)") {
    renderElement("dialect13a.yaml", "example13a.yaml", encodes, "example13a-encodes.yaml")
  }

  test("render 13b (test union)") {
    renderElement("dialect13b.yaml", "example13b.yaml", encodes, "example13b-encodes.yaml")
  }

  test("render 13c (test union with extends)") {
    renderElement("dialect13c.yaml", "example13c.yaml", encodes, "example13c-encodes.yaml")
  }

  test("render 16 test") {
    for {
      loadedConfig <- AMLConfiguration.predefined().withDialect(s"file://$basePath/dialect16b.yaml")
      assertion <- renderElement("dialect16a.yaml",
                                 "example16a.yaml",
                                 encodes,
                                 "example16a-encodes.yaml",
                                 baseConfig = loadedConfig)
    } yield {
      assertion
    }
  }

  test("render 24b test") {
    renderElement("dialect24.yaml", "example24b.yaml", encodes, "example24b-encodes.yaml")
  }

  test("render 16 $include test") {
    for {
      loadedConfig <- AMLConfiguration.predefined().withDialect(s"file://$basePath/dialect16b.yaml")
      assertion <- renderElement("dialect16a.yaml",
                                 "example16c.yaml",
                                 encodes,
                                 "example16c-encodes.yaml",
                                 baseConfig = loadedConfig)
    } yield {
      assertion
    }
  }

  test("render 27a test") {
    renderElement("dialect27.yaml", "example27a.yaml", encodes, "example27a-encodes.yaml")
  }

  test("render 29 test - keyproperty") {
    renderElement("dialect29.yaml", "example29.yaml", encodes, "example29-encodes.yaml")
  }
}
