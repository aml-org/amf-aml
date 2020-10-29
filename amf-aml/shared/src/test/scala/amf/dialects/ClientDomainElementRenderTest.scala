package amf.dialects

import amf.client.convert.VocabulariesClientConverter._
import amf.client.model.document.{Dialect => ClientDialect}
import amf.client.model.domain.{DomainElement => ClientDomainElement}
import amf.client.parse.DefaultErrorHandler
import amf.client.render.{AmlDomainElementEmitter => ClientAmlDomainElementEmitter}
import amf.client.resolve.ClientErrorHandlerConverter.ErrorHandlerConverter
import amf.core.model.document.{BaseUnit, EncodesModel}
import amf.core.model.domain.DomainElement
import amf.core.remote.{Hint, VocabularyYamlHint}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceUnit}
import org.yaml.builder.YamlOutputBuilder

import scala.concurrent.ExecutionContext

class ClientDialectDomainElementRenderTest extends ClientDomainElementTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override val basePath: String                            = "amf-aml/shared/src/test/resources/vocabularies2/instances/"
  override val baseHint: Hint                              = VocabularyYamlHint
  val rendering: String                                    = "amf-aml/shared/src/test/resources/vocabularies2/rendering"

  val encodes: BaseUnit => Option[DomainElement] = {
    case e: EncodesModel =>
      Some(e.encodes)
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

  test("render 13b (test union)") {
    renderElement("dialect13b.yaml", "example13b.yaml", encodes, "example13b-encodes.yaml")
  }
}


trait ClientDomainElementTests extends DomainElementCycleTests {
  override def renderDomainElement(element: Option[DomainElement], instance: DialectInstanceUnit, dialect: Dialect): String = {
    val eh = ErrorHandlerConverter.asClient(DefaultErrorHandler())
    element.map { element =>
      val stringBuilder = YamlOutputBuilder()
      val clientDialect: ClientDialect = dialect
      val clientElement: ClientDomainElement = element
      ClientAmlDomainElementEmitter.emitToBuilder(clientElement, clientDialect, eh, stringBuilder)
      stringBuilder.result.toString
    }.getOrElse("")
  }
}
