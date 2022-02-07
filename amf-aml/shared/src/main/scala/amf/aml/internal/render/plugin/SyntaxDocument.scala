package amf.aml.internal.render.plugin

import amf.aml.client.scala.model.document.kind.{AMLDocumentKind, Dialect, DialectLibrary, Vocabulary}
import amf.aml.internal.parse.hints.AmlHeaders.{DIALECT, DIALECT_LIBRARY, VOCABULARY}
import amf.aml.internal.render.emitters.dialects.DocumentCreator
import amf.core.internal.remote.Mimes.`application/json`
import amf.core.internal.render.BaseEmitters.traverse
import amf.core.internal.render.emitters.EntryEmitter
import org.yaml.model.YDocument

object SyntaxDocument {
  def getFor(syntax: String, `type`: AMLDocumentKind): DocumentCreator = {
    val header = HeaderForAMLKind(`type`, syntax)
    syntax match {
      case `application/json` => JsonAmlDocument(header)
      case _                  => YamlAmlDocument(header)
    }
  }
}

object JsonAmlDocument {
  def apply(header: String): DocumentCreator = (entries: Seq[EntryEmitter]) => {
    YDocument(b => {
      b.obj { b =>
        b.entry("$type", header)
        traverse(entries, b)
      }
    })
  }
}

object YamlAmlDocument {
  def apply(header: String): DocumentCreator = (entries: Seq[EntryEmitter]) => {
    YDocument(b => {
      b.comment(header)
      b.obj { b =>
        traverse(entries, b)
      }
    })
  }
}

object HeaderForAMLKind {
  def apply(kind: AMLDocumentKind, mediaType: String): String = (kind, mediaType) match {
    case (Dialect, `application/json`)        => dropPercent(DIALECT)
    case (Dialect, _)                         => DIALECT
    case (DialectLibrary, `application/json`) => dropPercent(DIALECT_LIBRARY)
    case (DialectLibrary, _)                  => DIALECT_LIBRARY
    case (Vocabulary, `application/json`)     => dropPercent(VOCABULARY)
    case (Vocabulary, _)                      => VOCABULARY
    case _                                    => ""
  }

  private def dropPercent(header: String): String = header.toList match {
    case '%' :: other => other.mkString
    case other        => other.mkString
  }
}
