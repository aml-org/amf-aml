package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.emitter.BaseEmitters.{pos, traverse}
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.parser.Position
import amf.plugins.document.vocabularies.emitters.instances.NodeMappableFinder
import amf.plugins.document.vocabularies.metamodel.domain.SemanticExtensionModel
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.SemanticExtension
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YType

case class ExtensionMappingsEntryEmitter(dialect: Dialect,
                                         aliases: Map[String, (String, String)],
                                         ordering: SpecOrdering)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with GroupPosition {

  override def emit(b: EntryBuilder): Unit = {
    b.entry(
        "extensions",
        _.obj { b =>
          val results = extensions.map(ext => SemanticExtensionEmitter(dialect, ext, aliases, ordering))
          traverse(ordering.sorted(results), b)
        }
    )
  }

  override def position(): Position = groupPosition(extensions)

  private def extensions = dialect.extensions()
}

case class SemanticExtensionEmitter(dialect: Dialect,
                                    element: SemanticExtension,
                                    aliases: Map[String, (String, String)],
                                    ordering: SpecOrdering)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AliasEmitter {
  override def emit(b: EntryBuilder): Unit = {
    val emitters = emitAlias(element.extensionName().value(),
                             element.extensionMappingDefinition(),
                             SemanticExtensionModel.ExtensionMappingDefinition,
                             YType.Str).toSeq
    traverse(ordering.sorted(emitters), b)
  }

  override def position(): Position = pos(element.annotations)
}
