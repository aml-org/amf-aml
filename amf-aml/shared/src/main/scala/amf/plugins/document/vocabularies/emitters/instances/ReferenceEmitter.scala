package amf.plugins.document.vocabularies.emitters.instances
import amf.core.internal.render.BaseEmitters.MapEntryEmitter
import amf.core.internal.render.emitters.EntryEmitter
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.internal.render.SpecOrdering
import org.yaml.model.YDocument.EntryBuilder

case class ReferenceEmitter(reference: BaseUnit, ordering: SpecOrdering, aliases: Map[String, (String, String)])
    extends EntryEmitter {

  override def emit(b: EntryBuilder): Unit = {
    aliases.get(reference.id) match {
      case Some((alias, location)) =>
        MapEntryEmitter(alias, location).emit(b)
      case _ => // TODO: emit violation
    }
  }

  override def position(): Position = ZERO
}
