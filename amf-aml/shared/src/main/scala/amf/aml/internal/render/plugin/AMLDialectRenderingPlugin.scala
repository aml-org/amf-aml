package amf.aml.internal.render.plugin

import amf.aml.client.scala.model.document.{Dialect, DialectFragment, DialectLibrary}
import amf.aml.internal.render.emitters.dialects.{DialectEmitter, RamlDialectLibraryEmitter}
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.render.{AMFRenderPlugin, RenderConfiguration, RenderInfo}
import amf.core.internal.remote.Mimes
import amf.core.internal.remote.Mimes._
import org.yaml.builder.{DocBuilder, YDocumentBuilder}

class AMLDialectRenderingPlugin extends AMFRenderPlugin {
  override def emit[T](unit: BaseUnit, builder: DocBuilder[T], renderConfiguration: RenderConfiguration): Boolean = {
    builder match {
      case sb: YDocumentBuilder =>
        emit(unit, renderConfiguration) exists { doc =>
          sb.document = doc
          true
        }
      case _ => false
    }
  }

  private def emit(unit: BaseUnit, renderConfiguration: RenderConfiguration) = {
    // TODO ARM: Fragment????
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
}
