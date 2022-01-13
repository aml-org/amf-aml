package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.AnnotationMapping
import amf.aml.internal.registries.AMLRegistry
import amf.aml.internal.render.emitters.instances.{
  DefaultNodeMappableFinder,
  NodeMappableFinder,
  SemanticExtensionNodeEmitter
}
import amf.aml.internal.semantic.SemanticExtensionOps.findExtensionMapping
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter

class SemanticExtensionRenderer(finder: ExtensionDialectFinder, registry: AMLRegistry) {

  def render(key: String,
             extension: DomainExtension,
             parentTypes: Seq[String],
             ordering: SpecOrdering,
             renderOptions: RenderOptions): Option[EntryEmitter] = {
    findMappingThatDefinesExtension(extension, parentTypes)
      .flatMap {
        case (mapping, dialect) =>
          val finder = DefaultNodeMappableFinder(dialect)
          SemanticExtensionNodeEmitter(extension, mapping, dialect, ordering, renderOptions, registry, key)(finder)
            .emitField(mapping.toField())
      }
  }

  private def findMappingThatDefinesExtension(extension: DomainExtension,
                                              parentTypes: Seq[String]): Option[(AnnotationMapping, Dialect)] = {
    val maybeName = Option(extension.definedBy).flatMap(_.name.option())
    maybeName
      .flatMap(name => findExtensionMapping(name, parentTypes, finder))
  }
}
