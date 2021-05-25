package amf.client.remod.rendering

import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.client.remod.amfcore.plugins.render.{AMFRenderPlugin, RenderInfo}
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.emitters.vocabularies.VocabularyEmitter
import amf.plugins.document.vocabularies.model.document.Vocabulary
import org.yaml.builder.{DocBuilder, YDocumentBuilder}

class AMLVocabularyRenderingPlugin extends AMFRenderPlugin {
  override def emit[T](unit: BaseUnit,
                       builder: DocBuilder[T],
                       renderOptions: RenderOptions,
                       errorHandler: AMFErrorHandler): Boolean = {
    builder match {
      case sb: YDocumentBuilder =>
        emit(unit, renderOptions, errorHandler) exists { doc =>
          sb.document = doc
          true
        }
      case _ => false
    }
  }

  private def emit(unit: BaseUnit, renderOptions: RenderOptions, errorHandler: AMFErrorHandler) = {
    unit match {
      case vocabulary: Vocabulary => Some(VocabularyEmitter(vocabulary).emitVocabulary())
      case _                      => None
    }
  }

  override val id: String = "vocabulary-rendering-plugin"

  override def applies(element: RenderInfo): Boolean = element.unit.isInstanceOf[Vocabulary]

  override def priority: PluginPriority = NormalPriority

  override def defaultSyntax(): String = "application/yaml"

  override def mediaTypes: Seq[String] = Seq("application/aml", "application/aml+yaml")

}
