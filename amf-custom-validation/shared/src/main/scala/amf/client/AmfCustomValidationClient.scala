package amf.client

import amf.client.`new`.{AmfEnvironment, AmlEnvironment}
import amf.client.remote.Content
import amf.internal.resource.ResourceLoader
import amf.plugins.features.validation.custom.model.ValidationDialectText

import scala.concurrent.Future

object AmfCustomValidationClient {

  val url = "http://a.ml/dialects/profile.raml"

  private val validationProfileResourceLoader = new ResourceLoader {
    /** Fetch specified resource and return associated content. Resource should have benn previously accepted. */
    override def fetch(resource: String): Future[Content] = Future.successful(new Content(ValidationDialectText.text, resource))

    /** Accepts specified resource. */
    override def accepts(resource: String): Boolean = resource == url
  }


  def apply(): Future[AmfEnvironment] = {
    AmlEnvironment().withResourceLoader(validationProfileResourceLoader).withDialect(url)
  }
}
