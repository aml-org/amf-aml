package amf.plugins.document

import amf.client.convert.VocabulariesClientConverter._
import amf.client.environment.Environment
import amf.client.model.document.Dialect
import amf.core.unsafe.PlatformSecrets

import amf.plugins.domain.VocabulariesRegister

import scala.concurrent.ExecutionContext
import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
object Vocabularies extends PlatformSecrets {

  def register(): Unit = {
    VocabulariesRegister.register(platform)
  }
}
