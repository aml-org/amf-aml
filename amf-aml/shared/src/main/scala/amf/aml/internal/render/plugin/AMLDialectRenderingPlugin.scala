package amf.aml.internal.render.plugin

import amf.aml.client.scala.model.document.{Dialect, DialectFragment, DialectLibrary}
import amf.aml.internal.render.emitters.dialects.{DialectEmitter, RamlDialectLibraryEmitter}
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.render.{
  AMFRenderPlugin,
  RenderConfiguration,
  RenderInfo,
  SYAMLASTBuilder,
  SYAMLBasedRenderPlugin
}
import amf.core.internal.plugins.syntax.ASTBuilder
import amf.core.internal.remote.Mimes
import amf.core.internal.remote.Mimes._
import org.yaml.builder.{DocBuilder, YDocumentBuilder}
import org.yaml.model.YDocument

class AMLDialectRenderingPlugin extends SYAMLBasedRenderPlugin {
  override def emit[T](unit: BaseUnit, builder: ASTBuilder[T], renderConfiguration: RenderConfiguration): Boolean = {
    builder match {
      case sb: SYAMLASTBuilder =>
        val maybeDocument: Option[YDocument] = emitDoc(unit, renderConfiguration)
        maybeDocument.exists { doc =>
          sb.document = doc
          true
        }
      case _ => false
    }
  }

  private def emitDoc(unit: BaseUnit, renderConfiguration: RenderConfiguration) = {
    // TODO: Fragment????
    val dialects = renderConfiguration.renderPlugins.collect {
      case plugin: AMLDialectInstanceRenderingPlugin => plugin.dialect
    }
    val finder = DefaultNodeMappableFinder(dialects)
    unit match {
      case dialect: Dialect        => Some(DialectEmitter(dialect)(finder).emitDialect())
      case library: DialectLibrary => Some(RamlDialectLibraryEmitter(library)(finder).emitDialectLibrary())
      case _                       => None
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

  override def mediaTypes: Seq[String] = Seq(`application/yaml`)

  override protected def unparseAsYDocument(unit: BaseUnit,
                                            renderOptions: RenderOptions,
                                            errorHandler: AMFErrorHandler): Option[YDocument] =
    throw new UnsupportedOperationException("Unreachable code")
}
