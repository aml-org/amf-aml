package amf.aml.internal.render.emitters.dialects

import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.model.document.DeclaresModel
import amf.core.internal.render.BaseEmitters.MapEntryEmitter
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.aml.client.scala.model.document.Vocabulary
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
