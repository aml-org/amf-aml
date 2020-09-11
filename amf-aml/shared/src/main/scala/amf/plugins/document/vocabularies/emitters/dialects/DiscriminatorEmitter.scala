package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.emitter.BaseEmitters.MapEntryEmitter
import amf.core.emitter.EntryEmitter
import amf.core.model.domain.{AmfScalar, DomainElement}
import amf.core.parser.Position
import amf.plugins.document.vocabularies.metamodel.domain.UnionNodeMappingModel
import amf.plugins.document.vocabularies.model.domain.NodeWithDiscriminator
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YType

trait DiscriminatorEmitter extends PosExtractor with AliasesConsumer {

  def emitDiscriminator[T <: DomainElement](nodeWithDiscriminator: NodeWithDiscriminator[T]): Seq[EntryEmitter] = {
    var emitters: Seq[EntryEmitter] = Seq()
    nodeWithDiscriminator.fields.entry(UnionNodeMappingModel.TypeDiscriminator) foreach { entry =>
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

    nodeWithDiscriminator.fields.entry(UnionNodeMappingModel.TypeDiscriminatorName) foreach { entry =>
      val value = entry.value.value.asInstanceOf[AmfScalar].value.toString
      val pos   = fieldPos(nodeWithDiscriminator, entry.field)
      emitters ++= Seq(MapEntryEmitter("typeDiscriminatorName", value, YType.Str, pos))
    }
    emitters
  }

}
