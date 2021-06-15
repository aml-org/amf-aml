package amf.aml.internal.render.emitters.dialects

import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.render.BaseEmitters.MapEntryEmitter
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.aml.internal.render.emitters.instances.NodeMappableFinder
import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.DocumentMapping
import org.yaml.model.YDocument.EntryBuilder

case class FragmentMappingEmitter(
    dialect: Dialect,
    fragment: DocumentMapping,
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)])(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AliasesConsumer {

  override def emit(b: EntryBuilder): Unit = {
    aliasFor(fragment.encoded().value()) match {
      case Some(alias) => MapEntryEmitter(fragment.documentName().value(), alias).emit(b)
      case _           => MapEntryEmitter(fragment.documentName().value(), fragment.encoded().value()).emit(b)
    }
  }

  override def position(): Position =
    fragment.annotations.find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)
}
