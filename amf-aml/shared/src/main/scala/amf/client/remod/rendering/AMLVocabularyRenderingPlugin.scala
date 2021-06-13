package amf.client.remod.rendering

import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.render.{AMFRenderPlugin, RenderConfiguration, RenderInfo}
import amf.plugins.document.vocabularies.emitters.vocabularies.VocabularyEmitter
import amf.plugins.document.vocabularies.model.document.Vocabulary
import org.yaml.builder.{DocBuilder, YDocumentBuilder}

class AMLVocabularyRenderingPlugin extends AMFRenderPlugin {
  override def emit[T](unit: BaseUnit, builder: DocBuilder[T], config: RenderConfiguration): Boolean = {
    builder match {
      case sb: YDocumentBuilder =>
        emit(unit, config) exists { doc =>
          sb.document = doc
          true
        }
      case _ => false
    }
  }

  private def emit(unit: BaseUnit, config: RenderConfiguration) = {
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
