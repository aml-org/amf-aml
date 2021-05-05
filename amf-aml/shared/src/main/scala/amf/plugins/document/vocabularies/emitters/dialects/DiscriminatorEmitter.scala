package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.emitter.BaseEmitters.MapEntryEmitter
import amf.core.emitter.EntryEmitter
import amf.core.model.domain.AmfScalar
import amf.core.parser.Position
import amf.plugins.document.vocabularies.metamodel.domain.NodeWithDiscriminatorModel
import amf.plugins.document.vocabularies.model.domain.NodeWithDiscriminator
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
