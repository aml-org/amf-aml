package amf.client.remod.rendering

import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.client.remod.amfcore.plugins.render.{AMFRenderPlugin, RenderInfo}
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectFragment, DialectLibrary}
import org.yaml.builder.DocBuilder

class AMLDialectRenderingPlugin extends AMFRenderPlugin {
  override def emit[T](unit: BaseUnit,
                       builder: DocBuilder[T],
                       renderOptions: RenderOptions,
                       errorHandler: AMFErrorHandler): Boolean =
    AMLPlugin.emit(unit, builder, renderOptions, errorHandler)

  override val id: String = "dialect-rendering-plugin"

  override def applies(element: RenderInfo): Boolean =
    element.unit match {
      case _: Dialect | _: DialectLibrary | _: DialectFragment => true
      case _                                                   => false
    }

  override def priority: PluginPriority = NormalPriority
}
