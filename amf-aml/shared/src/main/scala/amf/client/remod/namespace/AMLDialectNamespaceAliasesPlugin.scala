package amf.client.remod.namespace

import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.client.remod.amfcore.plugins.namespace.NamespaceAliasesPlugin
import amf.core.annotations.Aliases
import amf.core.annotations.Aliases.Alias
import amf.core.model.document.BaseUnit
import amf.core.vocabulary.{Namespace, NamespaceAliases}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceUnit}

case class AMLDialectNamespaceAliasesPlugin private (dialect: Dialect, aliases: NamespaceAliases)
    extends NamespaceAliasesPlugin {

  override def applies(element: BaseUnit): Boolean = element match {
    case instance: DialectInstanceUnit => instance.definedBy().option().contains(dialect.id)
    case currentDialect: Dialect       => currentDialect.id == dialect.id
    case _                             => false
  }

  override def aliases(_unit: BaseUnit): NamespaceAliases = aliases

  override val id: String = s"${dialect.id}/dialect-namespace-generation-plugin"

  override def priority: PluginPriority = NormalPriority
}

object AMLDialectNamespaceAliasesPlugin {
  def forDialect(dialect: Dialect): Option[AMLDialectNamespaceAliasesPlugin] = {

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
    val allAliases = externalAliases ++ annotAliases
    if (allAliases.nonEmpty)
      Some(
          AMLDialectNamespaceAliasesPlugin(
              dialect,
              NamespaceAliases.withCustomAliases((externalAliases ++ annotAliases).toMap)))
    else None
  }
}
