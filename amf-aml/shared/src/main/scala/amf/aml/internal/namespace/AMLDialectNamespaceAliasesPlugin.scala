package amf.aml.internal.namespace

import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.internal.annotations.Aliases
import amf.core.internal.annotations.Aliases.Alias
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.vocabulary.{Namespace, NamespaceAliases}
import amf.core.internal.plugins.namespace.NamespaceAliasesPlugin
import amf.aml.client.scala.model.document.{Dialect, DialectInstanceUnit}
import com.github.ghik.silencer.silent

case class AMLDialectNamespaceAliasesPlugin private (dialect: Dialect, aliases: NamespaceAliases)
    extends NamespaceAliasesPlugin {

  override def applies(element: BaseUnit): Boolean = element match {
    case instance: DialectInstanceUnit =>
      @silent("deprecated") // Silent can only be used in assignment expressions
      val a = instance.processingData.definedBy().option().orElse(instance.definedBy().option()).contains(dialect.id)
      a
    case currentDialect: Dialect => currentDialect.id == dialect.id
    case _                       => false
  }

  override def aliases(_unit: BaseUnit): NamespaceAliases = aliases

  override val id: String = s"${dialect.nameAndVersion()}/dialect-namespace-generation-plugin"

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
