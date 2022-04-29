package amf.aml.internal.render.plugin

import amf.aml.client.scala.model.document.{Dialect, DialectFragment, DialectLibrary, kind}
import amf.aml.internal.render.emitters.dialects.{DialectEmitter, RamlDialectLibraryEmitter}
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.render.{RenderConfiguration, RenderInfo, SYAMLASTBuilder, SYAMLBasedRenderPlugin}
import amf.core.internal.plugins.syntax.ASTBuilder
import amf.core.internal.remote.Mimes._
import org.yaml.model.YDocument

class AMLDialectRenderingPlugin extends SYAMLBasedRenderPlugin {
  override def emit[T](
      unit: BaseUnit,
      builder: ASTBuilder[T],
      renderConfiguration: RenderConfiguration,
      mediaType: String
  ): Boolean = {
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

  private def emitDoc(unit: BaseUnit, renderConfiguration: RenderConfiguration, mediaType: String) = {
    // TODO: Fragment????
    val dialects = getDialects(renderConfiguration)
    val finder   = DefaultNodeMappableFinder(dialects)
    unit match {
      case dialect: Dialect =>
        val doc = SyntaxDocument.getFor(mediaType, kind.Dialect)
        Some(DialectEmitter(dialect, doc)(finder).emitDialect())
      case library: DialectLibrary =>
        val doc = SyntaxDocument.getFor(mediaType, kind.DialectLibrary)
        Some(RamlDialectLibraryEmitter(library, doc)(finder).emitDialectLibrary())
      case _ => None
    }
  }

  private def getDialects(renderConfiguration: RenderConfiguration) = {
    renderConfiguration.renderPlugins.collect { case plugin: AMLDialectInstanceRenderingPlugin =>
      plugin.dialect
    }
  }

  override val id: String = "dialect-rendering-plugin"

  override def applies(element: RenderInfo): Boolean =
    element.unit match {
      case _: Dialect | _: DialectLibrary | _: DialectFragment => true
      case _                                                   => false
    }

  override def priority: PluginPriority = NormalPriority

  override def defaultSyntax(): String = `application/yaml`

  override def mediaTypes: Seq[String] = Seq(`application/yaml`, `application/json`)

  override protected def unparseAsYDocument(
      unit: BaseUnit,
      renderConfig: RenderConfiguration,
      errorHandler: AMFErrorHandler
  ): Option[YDocument] =
    throw new UnsupportedOperationException("Unreachable code")
}
