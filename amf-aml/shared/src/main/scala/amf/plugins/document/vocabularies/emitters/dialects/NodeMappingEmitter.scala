package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.render.BaseEmitters.{MapEntryEmitter, ScalarEmitter, traverse}
import amf.core.internal.render.emitters.EntryEmitter
import amf.core.client.scala.model.domain.{AmfScalar, DomainElement, Linkable}
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.internal.render.SpecOrdering
import amf.plugins.document.vocabularies.emitters.instances.NodeMappableFinder
import amf.plugins.document.vocabularies.metamodel.domain.{NodeMappableModel, UnionNodeMappingModel}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectFragment}
import amf.plugins.document.vocabularies.model.domain.{NodeMappable, NodeMapping, PropertyMapping, UnionNodeMapping}
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YNode

case class NodeMappingEmitter(
    dialect: Dialect,
    nodeMappable: NodeMappable[_ <: NodeMappableModel],
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)])(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with DiscriminatorEmitter
    with AliasesConsumer
    with PosExtractor {

  override def emit(b: EntryBuilder): Unit = {

    if (nodeMappable.isLink) {
      if (isFragment(nodeMappable.linkTarget.get, dialect)) {
        b.entry(nodeMappable.name.value(), YNode.include(nodeMappable.linkLabel.value()))
      } else {
        b.entry(nodeMappable.name.value(), nodeMappable.linkLabel.value())
      }
    } else {
      nodeMappable match {
        case nodeMapping: NodeMapping           => emitSingleNode(b, nodeMapping)
        case unionNodeMapping: UnionNodeMapping => emitUnioNode(b, unionNodeMapping)
        case _                                  => // ignore
      }
    }
  }

  protected def emitSingleNode(b: EntryBuilder, nodeMapping: NodeMapping): Unit = {
    b.entry(
        nodeMapping.name.value(),
        _.obj { b =>
          aliasFor(nodeMapping.nodetypeMapping.value()) match {
            case Some(classTermAlias) => MapEntryEmitter("classTerm", classTermAlias).emit(b)
            case None                 => nodeMapping.nodetypeMapping
          }
          nodeMapping.propertiesMapping() match {
            case properties: Seq[PropertyMapping] if properties.nonEmpty =>
              b.entry(
                  "mapping",
                  _.obj { b =>
                    val propertiesEmitters: Seq[PropertyMappingEmitter] = properties.map { pm: PropertyMapping =>
                      PropertyMappingEmitter(dialect, pm, ordering, aliases)
                    }
                    traverse(ordering.sorted(propertiesEmitters), b)
                  }
              )
            case _ => // ignore
          }
          nodeMapping.extend.headOption match {
            case Some(link: Linkable) =>
              b.entry("extends", link.linkLabel.value())
            case _ => // ignore
          }
          nodeMapping.idTemplate.option().foreach { idTemplate =>
            b.entry("idTemplate", idTemplate)
          }
          nodeMapping.mergePolicy.option().foreach { policy =>
            b.entry("patch", policy)
          }
        }
    )
  }

  protected def emitUnioNode(b: EntryBuilder, unionNodeMapping: UnionNodeMapping): Unit = {
    b.entry(
        unionNodeMapping.name.value(),
        _.obj { b =>
          var emitters: Seq[EntryEmitter] = Seq()
          val nodes                       = unionNodeMapping.objectRange()
          if (nodes.nonEmpty) {
            val pos = fieldPos(unionNodeMapping, UnionNodeMappingModel.ObjectRange)
            val targets = nodes
              .map { nodeId =>
                aliasFor(nodeId.value()) match {
                  case Some(nodeMappingAlias) => Some(nodeMappingAlias)
                  case _                      => None
                }
              }
              .collect { case Some(alias) => alias }

            emitters ++= Seq(new EntryEmitter {
              override def emit(b: EntryBuilder): Unit =
                b.entry("union", _.list { b =>
                  targets.foreach(target => ScalarEmitter(AmfScalar(target)).emit(b))
                })
              override def position(): Position = pos
            })

            emitters ++= emitDiscriminator(unionNodeMapping)

            ordering.sorted(emitters).foreach(_.emit(b))
          }
        }
    )
  }

  def isFragment(elem: DomainElement, dialect: Dialect): Boolean = {
    dialect.references.exists {
      case ref: DialectFragment => ref.encodes.id == elem.id
      case _                    => false
    }
  }

  override def position(): Position =
    nodeMappable
      .asInstanceOf[DomainElement]
      .annotations
      .find(classOf[LexicalInformation])
      .map(_.range.start)
      .getOrElse(ZERO)
}
