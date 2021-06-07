package amf.plugins.document

import amf.core.unsafe.PlatformSecrets
import amf.plugins.domain.VocabulariesRegister

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
object Vocabularies extends PlatformSecrets {

  def register(): Unit = {
    VocabulariesRegister.register(platform)
  }
}
