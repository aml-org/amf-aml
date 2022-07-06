package amf.aml.internal.render.emitters.dialects

import amf.core.internal.render.BaseEmitters.{pos, traverse}
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.aml.internal.render.emitters.instances.NodeMappableFinder
import amf.aml.internal.metamodel.domain.SemanticExtensionModel
import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.SemanticExtension
import org.mulesoft.common.client.lexical.Position
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YType

case class ExtensionMappingsEntryEmitter(
    dialect: Dialect,
    aliases: Map[String, (String, String)],
    ordering: SpecOrdering
)(implicit val nodeMappableFinder: NodeMappableFinder)
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

case class SemanticExtensionEmitter(
    dialect: Dialect,
    element: SemanticExtension,
    aliases: Map[String, (String, String)],
    ordering: SpecOrdering
)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AliasEmitter {
  override def emit(b: EntryBuilder): Unit = {
    val emitters = emitAlias(
        element.extensionName().value(),
        element.extensionMappingDefinition(),
        SemanticExtensionModel.ExtensionMappingDefinition,
        YType.Str
    ).toSeq
    traverse(ordering.sorted(emitters), b)
  }

  override def position(): Position = pos(element.annotations)
}
