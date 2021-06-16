package amf.aml.client.platform

import amf.aml.client.platform.model.document.Vocabulary
import amf.core.client.platform.AMFResult
import amf.aml.client.scala.{AMLVocabularyResult => InternalAMLVocabularyResult}
import scala.scalajs.js.annotation.JSExportAll
import amf.aml.internal.convert.VocabulariesClientConverter._

@JSExportAll
class AMLVocabularyResult(private[amf] override val _internal: InternalAMLVocabularyResult)
    extends AMFResult(_internal) {
  def vocabulary: Vocabulary = _internal.vocabulary
}
