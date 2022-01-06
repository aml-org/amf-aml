package amf.aml.internal.render.plugin

import amf.aml.client.scala.model.document.{Dialect, DialectInstanceUnit}
import amf.aml.internal.AMLDialectInstancePlugin
import amf.aml.internal.registries.AMLRegistry
import amf.aml.internal.render.emitters.instances.{DefaultNodeMappableFinder, DialectInstancesEmitter}
import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.render.{RenderConfiguration, RenderInfo, SYAMLASTBuilder, SYAMLBasedRenderPlugin}
import amf.core.internal.plugins.syntax.ASTBuilder
import amf.core.internal.remote.Mimes._
import com.github.ghik.silencer.silent
import org.yaml.builder.{DocBuilder, YDocumentBuilder}
import org.yaml.model.YDocument

/**
  * Parsing plugin for dialect instance like units derived from a resolved dialect
  * @param dialect resolved dialect
  */
class AMLDialectInstanceRenderingPlugin(val dialect: Dialect)
    extends SYAMLBasedRenderPlugin
    with AMLDialectInstancePlugin[RenderInfo] {
  override val id: String = s"${dialect.nameAndVersion()}/dialect-instances-rendering-plugin"

  override def priority: PluginPriority = NormalPriority

  override def emit[T](unit: BaseUnit, builder: ASTBuilder[T], config: RenderConfiguration): Boolean = {
    builder match {
      case sb: SYAMLASTBuilder =>
        unparse(unit, config) exists { doc =>
          sb.document = doc
          true
        }
      case _ => false
    }
  }

  private def unparse(unit: BaseUnit, config: RenderConfiguration) = {
    val dialects = config.renderPlugins.collect {
      case plugin: AMLDialectInstanceRenderingPlugin => plugin.dialect
    }
    val finder = DefaultNodeMappableFinder(dialects)
    unit match {
      case instance: DialectInstanceUnit =>
        Some(
            DialectInstancesEmitter(instance, dialect, config.renderOptions, AMLRegistry(config.registry, dialects))(
                finder).emitInstance())
      case _ => None
    }
  }

  override def applies(element: RenderInfo): Boolean = element.unit match {
    case unit: DialectInstanceUnit =>
      @silent("deprecated") // Silent can only be used in assignment expressions
      val a = unit.processingData.definedBy().option().orElse(unit.definedBy().option()).contains(dialect.id)
      a
    case _ => false
  }

  override def defaultSyntax(): String = `application/yaml`

  override def mediaTypes: Seq[String] =
    Seq(`application/yaml`, `application/json`)

  override protected def unparseAsYDocument(unit: BaseUnit,
                                            renderConfig: RenderConfiguration,
                                            errorHandler: AMFErrorHandler): Option[YDocument] =
    throw new UnsupportedOperationException("Unreachable code")
}
