package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect
import amf.aml.internal.registries.AMLRegistry
import amf.aml.internal.render.emitters.instances.{NodeFieldEmitters, NodeMappableFinder}
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.parse.document.ParserContext
import amf.core.internal.annotations.{LexicalInformation, SourceAST}
import amf.core.internal.parser.ParseConfiguration
import amf.core.internal.plugins.render.RenderConfiguration
import amf.core.internal.render.SpecOrdering
import org.yaml.model.YMapEntry

class SemanticExtensionsFacade private (annotation: String,
                                        val registry: AMLRegistry,
                                        specAnnotationValidator: AnnotationSchemaValidator) {

  private val finder   = CachedExtensionDialectFinder(registry)
  private val parser   = new SemanticExtensionParser(finder, specAnnotationValidator)
  private val renderer = new SemanticExtensionRenderer(finder, registry)

  def parse(parentTypes: Seq[String],
            ast: YMapEntry,
            ctx: ParserContext,
            extensionId: String): Option[DomainExtension] = {
    parser.parse(annotation, parentTypes, ast, ctx, extensionId)
  }

  def render(extension: DomainExtension,
             parentTypes: Seq[String],
             ordering: SpecOrdering,
             renderOptions: RenderOptions) = {
    renderer.render(annotation, extension, parentTypes, ordering, renderOptions)
  }
}

trait SemanticExtensionsFacadeBuilder {

  def extensionName(name: String): SemanticExtensionsFacade // todo: replace for generic ast interface at graphql branch
}

object SemanticExtensionsFacade {
  def apply(extensionName: String, registry: AMLRegistry): SemanticExtensionsFacade =
    new SemanticExtensionsFacade(extensionName, registry, IgnoreAnnotationSchemaValidator)

  def apply(extensionName: String,
            dialect: Dialect,
            annotationSchemaValidator: AnnotationSchemaValidator): SemanticExtensionsFacade =
    new SemanticExtensionsFacade(extensionName,
                                 AMLRegistry.apply(AMLRegistry.empty, Seq(dialect)),
                                 annotationSchemaValidator)

  def apply(extensionName: String,
            config: ParseConfiguration,
            annotationSchemaValidator: AnnotationSchemaValidator): SemanticExtensionsFacade =
    config.registryContext.getRegistry match {
      case registry: AMLRegistry => new SemanticExtensionsFacade(extensionName, registry, annotationSchemaValidator)
      case other                 => new SemanticExtensionsFacade(extensionName, AMLRegistry(other), annotationSchemaValidator)
    }

  def apply(extensionName: String, config: ParseConfiguration): SemanticExtensionsFacade =
    SemanticExtensionsFacade.apply(extensionName, config, IgnoreAnnotationSchemaValidator)

  def apply(extensionName: String, config: RenderConfiguration): SemanticExtensionsFacade = config.registry match {
    case registry: AMLRegistry => SemanticExtensionsFacade(extensionName, registry)
    case other                 => SemanticExtensionsFacade(extensionName, AMLRegistry(other))
  }
}
