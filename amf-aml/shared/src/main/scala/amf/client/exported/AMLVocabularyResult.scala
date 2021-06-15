package amf.client.exported

import amf.client.environment.{AMLVocabularyResult => InternalAMLVocabularyResult}
import amf.client.model.document.Vocabulary
import amf.client.convert.VocabulariesClientConverter._
import amf.core.client.platform.AMFResult

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class AMLVocabularyResult(private[amf] override val _internal: InternalAMLVocabularyResult)
    extends AMFResult(_internal) {
  def vocabulary: Vocabulary = _internal.vocabulary
}
