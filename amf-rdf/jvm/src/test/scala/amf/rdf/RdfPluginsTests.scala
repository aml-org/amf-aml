package amf.rdf

import amf.core.client.common.remote.Content
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.ParsingOptions
import amf.core.client.scala.model.document.Document
import amf.core.client.scala.resource.ResourceLoader
import amf.core.internal.unsafe.PlatformSecrets
import amf.rdf.client.scala.RdfConfiguration
import amf.rdf.internal.unsafe.RdfPlatformSecrets
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.must.Matchers

import scala.concurrent.{ExecutionContext, Future}

class RdfPluginsTests extends AsyncFunSuite with RdfPlatformSecrets with Matchers {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  private val basePath = "file://amf-rdf/jvm/src/test/resources/"
  private val myLoader = InMemoryResourceLoader("file://api.raml")
  private val client = RdfConfiguration()
    .withParsingOptions(ParsingOptions().withoutAmfJsonLdSerialization)
    .withResourceLoaders(List(myLoader))
    .baseUnitClient()

  test("Parse plugin") {
    val path = basePath + "example.nt"
    for {
      parsed <- client.parse(path)
    } yield {
      val unit = parsed.baseUnit
      assert(!unit.asInstanceOf[Document].processingData.transformed.value())
      assert(unit.root.value())
      assert(unit.references.size == 1)
    }
  }

  test("Render plugin") {
    val path = basePath + "example.nt"
    for {
      parsed   <- client.parse(path)
      rendered <- Future.successful(client.render(parsed.baseUnit))
    } yield {
      assert(
        rendered.contains(
          "<file://api.raml#/BaseUnitProcessingData> <http://a.ml/vocabularies/document#transformed> \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean>"
        )
      )
      assert(rendered.contains("<file://api.raml> <http://a.ml/vocabularies/document#version> \"2.1.0\""))
      assert(
        rendered.contains(
          "file://api.raml> <http://a.ml/vocabularies/document#root> \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>"
        )
      )
    }
  }

  case class InMemoryResourceLoader(url: String) extends ResourceLoader with RdfPlatformSecrets with PlatformSecrets {

    override def accepts(resource: String): Boolean = true

    override def fetch(resource: String): Future[Content] = {
      for {
        content <- platform.fetchContent(resource, AMFGraphConfiguration.predefined())
      } yield {
        content.copy(url = url)
      }
    }
  }
}
