package amf.aml.internal.render.plugin

import amf.aml.client.scala.model.document.{Vocabulary, kind}
import amf.aml.internal.render.emitters.vocabularies.VocabularyEmitter
import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.render.{RenderConfiguration, RenderInfo, SYAMLASTBuilder, SYAMLBasedRenderPlugin}
import amf.core.internal.plugins.syntax.ASTBuilder
import amf.core.internal.remote.Mimes._
import org.yaml.model.YDocument

class AMLVocabularyRenderingPlugin extends SYAMLBasedRenderPlugin {

  override val id: String = "vocabulary-rendering-plugin"

  override def applies(element: RenderInfo): Boolean = element.unit.isInstanceOf[Vocabulary]

  override def priority: PluginPriority = NormalPriority

  override def defaultSyntax(): String = `application/yaml`

  override def mediaTypes: Seq[String] = Seq(`application/yaml`, `application/json`)

  override def emit[T](unit: BaseUnit,
                       builder: ASTBuilder[T],
                       renderConfiguration: RenderConfiguration,
                       mediaType: String): Boolean = {
    builder match {
      case sb: SYAMLASTBuilder =>
        val maybeDocument: Option[YDocument] = emitDoc(unit, renderConfiguration, mediaType)
        maybeDocument.exists { doc =>
          sb.document = doc
          true
        }
      case _ => false
    }
  }

  private def emitDoc(unit: BaseUnit, renderConfig: RenderConfiguration, mediaType: String): Option[YDocument] = {
    val doc = SyntaxDocument.getFor(mediaType, kind.Vocabulary)
    unit match {
      case vocabulary: Vocabulary => Some(VocabularyEmitter(vocabulary, doc).emitVocabulary())
      case _                      => None
    }
  }

  protected def unparseAsYDocument(unit: BaseUnit,
                                   renderConfig: RenderConfiguration,
                                   errorHandler: AMFErrorHandler): Option[YDocument] = {
    throw new UnsupportedOperationException("Unreachable code")
  }
}
