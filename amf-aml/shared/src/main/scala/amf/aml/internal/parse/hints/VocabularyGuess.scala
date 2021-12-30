package amf.aml.internal.parse.hints

import amf.aml.client.scala.model.document.kind
import amf.core.internal.parser.Root

import scala.util.matching.Regex

object VocabularyGuess extends Guess[kind.Vocabulary.type] {
  private lazy val vocabulary: Regex = "%? *Vocabulary *1\\.0 *".r

  override def from(root: Root): Option[kind.Vocabulary.type] = hint(root).flatMap(matchHint)

  override def hint(root: Root): Option[String] =
    YamlDirectiveComment.from(root).orElse($TypePropertyValue.from(root))

  private def matchHint(hint: String): Option[kind.Vocabulary.type] = {
    hint match {
      case vocabulary(_*) => Some(kind.Vocabulary)
      case _              => None
    }
  }
}
