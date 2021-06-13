package amf.client.remod.rendering

import amf.client.remod.AMLDialectInstancePlugin
import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.render.{AMFRenderPlugin, RenderConfiguration, RenderInfo}
import amf.plugins.document.vocabularies.emitters.dialects.{DialectEmitter, RamlDialectLibraryEmitter}
import amf.plugins.document.vocabularies.emitters.instances.{DefaultNodeMappableFinder, DialectInstancesEmitter}
import amf.plugins.document.vocabularies.emitters.vocabularies.VocabularyEmitter
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceUnit, DialectLibrary, Vocabulary}
import org.yaml.builder.{DocBuilder, YDocumentBuilder}

/**
  * Parsing plugin for dialect instance like units derived from a resolved dialect
  * @param dialect resolved dialect
  */
class AMLDialectInstanceRenderingPlugin(val dialect: Dialect)
    extends AMFRenderPlugin
    with AMLDialectInstancePlugin[RenderInfo] {
  override val id: String = s"${dialect.id}/dialect-instances-rendering-plugin"

  override def priority: PluginPriority = NormalPriority

  override def emit[T](unit: BaseUnit, builder: DocBuilder[T], config: RenderConfiguration): Boolean = {
    builder match {
      case sb: YDocumentBuilder =>
        unparse(unit, config) exists { doc =>
          sb.document = doc
          true
        }
      case _ => false
    }
  }

  private def unparse(unit: BaseUnit, config: RenderConfiguration) = {
    val dialects = config.renderPlugins.collect {
      case plugin: AMLDialectInstanceRenderingPlugin => plugin.dialect
    }
    val finder = DefaultNodeMappableFinder(dialects)
    unit match {
      case instance: DialectInstanceUnit =>
        Some(DialectInstancesEmitter(instance, dialect, config.renderOptions)(finder).emitInstance())
      case _ => None
    }
  }

  override def applies(element: RenderInfo): Boolean = element.unit match {
    case unit: DialectInstanceUnit => unit.definedBy().option().contains(dialect.id)
    case _                         => false
  }

  override def defaultSyntax(): String = "application/yaml"

  override def mediaTypes: Seq[String] =
    Seq("application/aml", "application/yaml", "application/aml+yaml", "application/json", "application/aml+json")
}
