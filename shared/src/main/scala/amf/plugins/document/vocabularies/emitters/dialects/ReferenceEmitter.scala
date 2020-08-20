package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.emitter.BaseEmitters.MapEntryEmitter
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.model.document.DeclaresModel
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.model.document.Vocabulary
import org.yaml.model.YDocument.EntryBuilder

case class ReferenceEmitter(reference: DeclaresModel, ordering: SpecOrdering, aliases: Map[String, (String, String)])
    extends EntryEmitter {

  override def emit(b: EntryBuilder): Unit = {
    val aliasKey = reference match {
      case vocabulary: Vocabulary => vocabulary.base.value()
      case _                      => reference.id
    }
    aliases.get(aliasKey) match {
      case Some((alias, location)) =>
        MapEntryEmitter(alias, location).emit(b)
      case _ => // TODO: emit violation
    }
  }

  override def position(): Position = ZERO
}
