package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.LexicalInformation
import amf.core.emitter.BaseEmitters.traverse
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.model.domain.DomainElement
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.emitters.instances.NodeMappableFinder
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, NodeMappable}
import org.yaml.model.YDocument.EntryBuilder

case class NodeMappingsEntryEmitter(dialect: Dialect,
                                    nodeMappingDeclarations: Seq[NodeMappable.AnyNodeMappable],
                                    aliases: Map[String, (String, String)],
                                    ordering: SpecOrdering)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with GroupPosition {

  override def emit(b: EntryBuilder): Unit = {
    b.entry(
        "nodeMappings",
        _.obj { b =>
          val nodeMappingEmitters = nodeMappingDeclarations.map { n =>
            NodeMappingEmitter(dialect, n, ordering, aliases)
          }
          traverse(ordering.sorted(nodeMappingEmitters), b)
        }
    )
  }

  override def position(): Position = groupPosition(nodeMappingDeclarations)
}
