package amf.aml.internal.namespace

import amf.aml.client.scala.model.document.Dialect
import amf.core.client.scala.vocabulary.{Namespace, NamespaceAliases}
import amf.core.internal.annotations.{Aliases, ReferencedInfo}
import amf.core.internal.annotations.Aliases.Alias

object DialectNamespaceAliases {
  def apply(dialect: Dialect): NamespaceAliases = {
    val externalAliases: Seq[(String, Namespace)] = dialect.externals.map { external =>
      external.alias.value() -> Namespace(external.base.value())
    }

    val annotAliases: Seq[(Alias, Namespace)] = dialect.annotations
      .find(classOf[Aliases])
      .map { aliasesAnnotation =>
        aliasesAnnotation.aliases.toList.map {
          case (alias, ReferencedInfo(_, url, _)) => alias -> Namespace(url)
        }
      }
      .getOrElse(Nil)

    NamespaceAliases((externalAliases ++ annotAliases).toMap)
  }
}
