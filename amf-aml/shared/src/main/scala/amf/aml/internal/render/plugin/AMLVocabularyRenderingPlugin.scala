package amf.aml.internal.render.plugin

import amf.aml.client.scala.model.document.Vocabulary
import amf.aml.internal.render.emitters.vocabularies.VocabularyEmitter
import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.render.{RenderInfo, SYAMLBasedRenderPlugin}
import amf.core.internal.remote.Mimes._
import org.yaml.model.YDocument

class AMLVocabularyRenderingPlugin extends SYAMLBasedRenderPlugin {

  override val id: String = "vocabulary-rendering-plugin"

  override def applies(element: RenderInfo): Boolean = element.unit.isInstanceOf[Vocabulary]

  override def priority: PluginPriority = NormalPriority

  override def defaultSyntax(): String = `application/yaml`

  override def mediaTypes: Seq[String] = Seq(`application/yaml`)

  override protected def unparseAsYDocument(unit: BaseUnit,
                                            renderOptions: RenderOptions,
                                            errorHandler: AMFErrorHandler): Option[YDocument] = {
    unit match {
      case vocabulary: Vocabulary => Some(VocabularyEmitter(vocabulary).emitVocabulary())
      case _                      => None
    }
  }
}
