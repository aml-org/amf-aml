package amf.aml.internal.parse.hints

import amf.aml.client.scala.model.document.kind
import amf.core.internal.parser.Root

import scala.util.matching.Regex

object DialectGuess extends Guess[kind.DialectDocumentKind] {
  private lazy val dialect: Regex  = "%? *Dialect *1\\.0 *".r
  private lazy val fragment: Regex = "%? *Node *Mapping */ *Dialect *1\\.0 *".r
  private lazy val library: Regex  = "%? *Library */ *Dialect *1\\.0 *".r

  override def from(root: Root): Option[kind.DialectDocumentKind] = hint(root).flatMap(matchHint)

  override def hint(root: Root): Option[String] = YamlDirectiveComment.from(root).orElse($DialectPropertyValue.from(root))

  private def matchHint(hint: String): Option[kind.DialectDocumentKind] = {
    hint match {
      case fragment(_*) => Some(kind.DialectFragment)
      case library(_*)  => Some(kind.DialectLibrary)
      case dialect(_*)  => Some(kind.Dialect)
      case _            => None
    }
  }
}
