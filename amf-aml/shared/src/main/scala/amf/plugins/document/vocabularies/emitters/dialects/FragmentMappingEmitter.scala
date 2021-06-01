package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.LexicalInformation
import amf.core.emitter.BaseEmitters.MapEntryEmitter
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.emitters.instances.NodeMappableFinder
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.DocumentMapping
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
