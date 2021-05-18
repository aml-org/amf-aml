package amf.client.remod.namespace

import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.client.remod.amfcore.plugins.namespace.NamespaceAliasesPlugin
import amf.core.annotations.Aliases
import amf.core.annotations.Aliases.Alias
import amf.core.model.document.BaseUnit
import amf.core.vocabulary.{Namespace, NamespaceAliases}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceUnit}

case class AMLDialectNamespaceAliasesPlugin(dialect: Dialect) extends NamespaceAliasesPlugin {

  override def applies(element: BaseUnit): Boolean = element match {
    case instance: DialectInstanceUnit => instance.definedBy().option().contains(dialect.id)
    case currentDialect: Dialect       => currentDialect.id == dialect.id
    case _                             => false
  }

  override def aliases(_unit: BaseUnit): NamespaceAliases = aliases

  lazy val aliases: NamespaceAliases = {
    val externalAliases: Seq[(String, Namespace)] = dialect.externals.map { external =>
      external.alias.value() -> Namespace(external.base.value())
    }

    val annotAliases: Seq[(Alias, Namespace)] = dialect.annotations
      .find(classOf[Aliases])
      .map { aliasesAnnotation =>
        aliasesAnnotation.aliases.toList.map {
          case (alias, (url, _)) => alias -> Namespace(url)
        }
      }
      .getOrElse(Nil)

    NamespaceAliases.withCustomAliases((externalAliases ++ annotAliases).toMap)
  }

  override val id: String = s"${dialect.id}/dialect-namespace-generation-plugin"

  override def priority: PluginPriority = NormalPriority
}
