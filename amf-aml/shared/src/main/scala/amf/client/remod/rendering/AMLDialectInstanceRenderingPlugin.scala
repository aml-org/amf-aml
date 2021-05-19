package amf.client.remod.rendering

import amf.client.remod.AMLDialectInstancePlugin
import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.render.{AMFRenderPlugin, RenderInfo}
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceUnit}
import org.yaml.builder.DocBuilder

/**
  * Parsing plugin for dialect instance like units derived from a resolved dialect
  * @param dialect resolved dialect
  */
class AMLDialectInstanceRenderingPlugin(val dialect: Dialect)
    extends AMFRenderPlugin
    with AMLDialectInstancePlugin[RenderInfo] {
  override val id: String = s"${dialect.id}/dialect-instances-rendering-plugin"

  override def priority: PluginPriority = NormalPriority

  override def emit[T](unit: BaseUnit,
                       builder: DocBuilder[T],
                       renderOptions: RenderOptions,
                       errorHandler: AMFErrorHandler): Boolean =
    AMLPlugin.emit(unit, builder, renderOptions, errorHandler)

  override def applies(element: RenderInfo): Boolean = element.unit match {
    case unit: DialectInstanceUnit => unit.definedBy().option().contains(dialect.id)
    case _                         => false
  }
}
