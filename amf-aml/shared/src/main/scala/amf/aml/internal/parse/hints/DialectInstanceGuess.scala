package amf.aml.internal.parse.hints

import amf.aml.client.scala.model.document.kind.DialectInstanceDocumentKind
import amf.aml.client.scala.model.document.{Dialect, kind}
import amf.core.internal.parser.Root

import scala.util.matching.Regex

case class DialectInstanceGuess(dialect: Dialect) extends Guess[kind.DialectInstanceDocumentKind] {

  private val nameR: String    = dialect.name().value()
  private val versionR: String = dialect.version().value().replace(".", "\\.")

  private lazy val instance: Regex = (s"%? *$nameR *$versionR *").r
  private lazy val library: Regex  = (s"%? *Library */ *$nameR *$versionR *").r
  private lazy val patch: Regex    = (s"%? *Patch */ *$nameR *$versionR *").r
  private lazy val fragments: Seq[Regex] = {
    dialect
      .documents()
      .fragments()
      .map { f =>
        val fragmentNameR = f.documentName().value()
        (s"%? *$fragmentNameR */ *$nameR *$versionR *").r
      }
  }

  override def from(root: Root): Option[kind.DialectInstanceDocumentKind] = {
    if (dialect.usesKeyPropertyMatching) {
      matchByKeyProperty(root)
    } else {
      matchByHint(root)
    }
  }

  override def hint(root: Root): Option[String] =
    YamlDirectiveComment.from(root).orElse($DialectPropertyValue.from(root))

  private def matchByHint(root: Root): Option[kind.DialectInstanceDocumentKind] = {
    for {
      hint <- hint(root)
      kind <- {
        hint.split(" *\\| *") match {
          case Array(hint, dialectUri) => matchByHintAndUri(hint, dialectUri)
          case _                       => matchHint(hint)
        }
      }
    } yield {
      kind
    }
  }

  def matchHint(hint: String): Option[DialectInstanceDocumentKind] = {
    hint match {
      case library(_*)                                                             => Some(kind.DialectInstanceLibrary)
      case patch(_*)                                                               => Some(kind.DialectInstancePatch)
      case instance(_*)                                                            => Some(kind.DialectInstance)
      case _ if fragments.exists(fragment => fragment.findFirstIn(hint).isDefined) => Some(kind.DialectInstanceFragment)
      case _                                                                       => None
    }
  }

  private def matchByKeyProperty(root: Root): Option[kind.DialectInstanceDocumentKind] = {
    val name    = dialect.name().value()
    val version = dialect.version().value()

    val keyPropertyValue = new PropertyValue {
      override val key: String = name
    }

    // We can only define dialect instances (not fragments, etc.) with key properties
    keyPropertyValue.from(root) match {
      case Some(`version`) => Some(kind.DialectInstance)
      case _               => None
    }
  }

  private def matchByHintAndUri(hint: String, dialectUri: String) = {
    if (dialectUri.stripPrefix("<").stripSuffix(">") == dialect.id) {
      matchHint(hint)
    } else {
      None
    }
  }
}
