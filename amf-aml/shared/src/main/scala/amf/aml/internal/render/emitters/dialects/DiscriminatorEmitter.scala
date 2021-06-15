package amf.aml.internal.render.emitters.dialects

import amf.core.internal.render.BaseEmitters.MapEntryEmitter
import amf.core.internal.render.emitters.EntryEmitter
import amf.core.client.scala.model.domain.AmfScalar
import amf.core.client.common.position.Position
import amf.aml.internal.metamodel.domain.NodeWithDiscriminatorModel
import amf.aml.client.scala.model.domain.NodeWithDiscriminator
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YType

trait DiscriminatorEmitter extends PosExtractor with AliasesConsumer {

  def emitDiscriminator[M <: NodeWithDiscriminatorModel](
      nodeWithDiscriminator: NodeWithDiscriminator[M]): Seq[EntryEmitter] = {
    var emitters: Seq[EntryEmitter] = Seq()
    nodeWithDiscriminator.fields.entry(nodeWithDiscriminator.meta.TypeDiscriminator) foreach { entry =>
      val pos          = fieldPos(nodeWithDiscriminator, entry.field)
      val typesMapping = nodeWithDiscriminator.typeDiscriminator()
      emitters ++= Seq(new EntryEmitter {
        override def emit(b: EntryBuilder): Unit =
          b.entry(
              "typeDiscriminator",
              _.obj { b =>
                typesMapping.foreach {
                  case (alias, nodeMappingId) =>
                    aliasFor(nodeMappingId) match {
                      case Some(nodeMapping) => b.entry(alias, nodeMapping)
                      case _                 => b.entry(alias, nodeMappingId)
                    }
                }
              }
          )

        override def position(): Position = pos
      })
    }

    nodeWithDiscriminator.fields.entry(nodeWithDiscriminator.meta.TypeDiscriminatorName) foreach { entry =>
      val value = entry.value.value.asInstanceOf[AmfScalar].value.toString
      val pos   = fieldPos(nodeWithDiscriminator, entry.field)
      emitters ++= Seq(MapEntryEmitter("typeDiscriminatorName", value, YType.Str, pos))
    }
    emitters
  }

}
