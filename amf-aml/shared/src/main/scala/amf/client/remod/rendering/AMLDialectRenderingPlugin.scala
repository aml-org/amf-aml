package amf.client.remod.rendering

import amf.client.remod.amfcore.config.RenderOptions
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.client.remod.amfcore.plugins.render.{AMFRenderPlugin, RenderInfo}
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.emitters.dialects.{DialectEmitter, RamlDialectLibraryEmitter}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectFragment, DialectLibrary}
import org.yaml.builder.{DocBuilder, YDocumentBuilder}

class AMLDialectRenderingPlugin extends AMFRenderPlugin {
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
    // TODO ARM: Fragment????
    unit match {
      case dialect: Dialect        => Some(DialectEmitter(dialect).emitDialect())
      case library: DialectLibrary => Some(RamlDialectLibraryEmitter(library).emitDialectLibrary())
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

  override def defaultSyntax(): String = "application/yaml"

  override def mediaTypes: Seq[String] = Seq("application/aml", "application/aml+yaml")
}
