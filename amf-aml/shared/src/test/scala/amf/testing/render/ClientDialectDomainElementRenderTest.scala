package amf.testing.render

import amf.core.client.scala.model.document.{BaseUnit, EncodesModel}
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.remote.{Hint, VocabularyYamlHint}
import amf.testing.unclassified.ClientDomainElementTests

import scala.concurrent.ExecutionContext

class ClientDialectDomainElementRenderTest extends ClientDomainElementTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override val basePath: String = "amf-aml/shared/src/test/resources/vocabularies2/instances/"
  override val baseHint: Hint   = VocabularyYamlHint
  val rendering: String         = "amf-aml/shared/src/test/resources/vocabularies2/rendering"

  val encodes: BaseUnit => Option[DomainElement] = {
    case e: EncodesModel =>
      Some(e.encodes)
    case _ => None
  }

  test("Simple node union rendering") {
    renderElement(
      "dialect.yaml",
      "instance.yaml",
      encodes,
      "instance-encodes.yaml",
      directory = s"$rendering/simple-node-union"
    )
  }

  test("render 1 (AMF) test") {
    renderElement("dialect1.yaml", "example1.yaml", encodes, "example1-encodes.yaml")
  }

  test("render 13b (test union)") {
    renderElement("dialect13b.yaml", "example13b.yaml", encodes, "example13b-encodes.yaml")
  }
}
