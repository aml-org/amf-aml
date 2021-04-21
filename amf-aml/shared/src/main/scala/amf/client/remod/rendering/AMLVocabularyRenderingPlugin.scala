package amf.client.remod.rendering

import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.client.remod.amfcore.plugins.render.{AMFRenderPlugin, RenderInfo}
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.Vocabulary
import org.yaml.builder.DocBuilder

class AMLVocabularyRenderingPlugin extends AMFRenderPlugin {
  override def emit[T](unit: BaseUnit,
                       builder: DocBuilder[T],
                       renderOptions: RenderOptions,
                       errorHandler: ErrorHandler): Boolean =
    AMLPlugin.emit(unit, builder, renderOptions, errorHandler)

  override val id: String = "vocabulary-rendering-plugin"

  override def applies(element: RenderInfo): Boolean = element.unit.isInstanceOf[Vocabulary]

  override def priority: PluginPriority = NormalPriority
}
