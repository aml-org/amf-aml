package amf.aml.internal.semantic

import amf.aml.internal.registries.AMLRegistry
import amf.aml.internal.render.emitters.instances.{NodeFieldEmitters, NodeMappableFinder}
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.parse.document.ParserContext
import amf.core.internal.parser.ParseConfiguration
import amf.core.internal.plugins.render.RenderConfiguration
import amf.core.internal.render.SpecOrdering
import org.yaml.model.YMapEntry

class SemanticExtensionsFacade private (val registry: AMLRegistry) {

  private val finder   = CachedExtensionDialectFinder(registry)
  private val parser   = new SemanticExtensionParser(finder)
  private val renderer = new SemanticExtensionRenderer(finder, registry)

  def parse(extensionName: String,
            parentTypes: Seq[String],
            ast: YMapEntry,
            ctx: ParserContext,
            extensionId: String): Option[DomainExtension] = {
    parser.parse(extensionName, parentTypes, ast, ctx, extensionId)
  }

  def render(key: String,
             extension: DomainExtension,
             parentTypes: Seq[String],
             ordering: SpecOrdering,
             renderOptions: RenderOptions) = {
    renderer.render(key, extension, parentTypes, ordering, renderOptions)
  }
}

object SemanticExtensionsFacade {
  def apply(registry: AMLRegistry): SemanticExtensionsFacade = new SemanticExtensionsFacade(registry)

  def apply(config: ParseConfiguration): SemanticExtensionsFacade = config.registryContext.getRegistry match {
    case registry: AMLRegistry => SemanticExtensionsFacade(registry)
    case other                 => SemanticExtensionsFacade(AMLRegistry(other))
  }

  def apply(config: RenderConfiguration): SemanticExtensionsFacade = config.registry match {
    case registry: AMLRegistry => SemanticExtensionsFacade(registry)
    case other                 => SemanticExtensionsFacade(AMLRegistry(other))
  }
}
