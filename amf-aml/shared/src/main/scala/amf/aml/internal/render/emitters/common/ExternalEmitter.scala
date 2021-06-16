package amf.aml.internal.render.emitters.common

import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.render.BaseEmitters.MapEntryEmitter
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.aml.client.scala.model.domain.External
import org.yaml.model.YDocument

case class ExternalEmitter(external: External, ordering: SpecOrdering) extends EntryEmitter {
  override def emit(b: YDocument.EntryBuilder): Unit =
    MapEntryEmitter(external.alias.value(), external.base.value()).emit(b)

  override def position(): Position =
    external.annotations.find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)
}
