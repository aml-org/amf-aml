package amf.aml.internal.render.emitters.dialects

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.NodeMappable
import amf.aml.internal.render.emitters.instances.NodeMappableFinder
import amf.core.client.common.position.Position
import amf.core.internal.render.BaseEmitters.traverse
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import org.yaml.model.YDocument.EntryBuilder

case class NodeMappingsEntryEmitter(
    dialect: Dialect,
    nodeMappingDeclarations: Seq[NodeMappable.AnyNodeMappable],
    aliases: Map[String, (String, String)],
    ordering: SpecOrdering
)(implicit val nodeMappableFinder: NodeMappableFinder)
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
