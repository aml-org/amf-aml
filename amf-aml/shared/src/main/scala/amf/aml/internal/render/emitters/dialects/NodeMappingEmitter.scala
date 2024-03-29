package amf.aml.internal.render.emitters.dialects

import amf.aml.client.scala.model.document.{Dialect, DialectFragment}
import amf.aml.client.scala.model.domain._
import amf.aml.internal.metamodel.domain.{NodeMappableModel, UnionNodeMappingModel}
import amf.aml.internal.render.emitters.instances.NodeMappableFinder
import amf.core.client.scala.model.domain.{AmfScalar, DomainElement, Linkable}
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.render.BaseEmitters.{MapEntryEmitter, ScalarEmitter, traverse}
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import org.mulesoft.common.client.lexical.Position
import org.mulesoft.common.client.lexical.Position.ZERO
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YNode

case class NodeMappingEmitter(
    dialect: Dialect,
    nodeMappable: NodeMappable[_ <: NodeMappableModel],
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)]
)(implicit val nodeMappableFinder: NodeMappableFinder)
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
        case unionNodeMapping: UnionNodeMapping => emitUnionNode(b, unionNodeMapping)
        case _                                  => // ignore
      }
    }
  }

  protected def emitSingleNode(b: EntryBuilder, nodeMapping: NodeMapping): Unit = {
    b.entry(
      nodeMapping.name.value(),
      _.obj { b =>
        emitAnyNode(b, nodeMapping)
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
        if (nodeMapping.extend.nonEmpty) {
          if (nodeMapping.extend.length == 1) {
            b.entry("extends", nodeMapping.extend.head.asInstanceOf[Linkable].linkLabel.value())
          } else {
            b.entry(
              "extends",
              l => {
                l.list { b =>
                  nodeMapping.extend.foreach { e =>
                    b.+=(YNode(e.asInstanceOf[Linkable].linkLabel.value()))
                  }
                }
              }
            )
          }
        }

        nodeMapping.closed.option().foreach { additionalProps =>
          b.entry("additionalProperties", !additionalProps)
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

  protected def emitUnionNode(b: EntryBuilder, unionNodeMapping: UnionNodeMapping): Unit = {
    b.entry(
      unionNodeMapping.name.value(),
      _.obj { b =>
        emitAnyNode(b, unionNodeMapping)
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
              b.entry(
                "union",
                _.list { b =>
                  targets.foreach(target => ScalarEmitter(AmfScalar(target)).emit(b))
                }
              )
            override def position(): Position = pos
          })

          emitters ++= emitDiscriminator(unionNodeMapping)

          ordering.sorted(emitters).foreach(_.emit(b))
        }
      }
    )
  }

  def emitAnyNode(b: EntryBuilder, anyMapping: AnyMapping): Unit = {
    if (anyMapping.and.nonEmpty) {
      val members = anyMapping.and.flatMap(member => aliasFor(member.value()))
      b.entry(
        "allOf",
        _.list(b => members.foreach(member => ScalarEmitter(AmfScalar(member)).emit(b)))
      )
    }
    if (anyMapping.or.nonEmpty) {
      val members = anyMapping.or.flatMap(member => aliasFor(member.value()))
      b.entry(
        "oneOf",
        _.list(b => members.foreach(member => ScalarEmitter(AmfScalar(member)).emit(b)))
      )
    }
    if (anyMapping.components.nonEmpty) {
      val members = anyMapping.components.flatMap(component => aliasFor(component.value()))
      b.entry(
        "components",
        _.list(b => members.foreach(component => ScalarEmitter(AmfScalar(component)).emit(b)))
      )
    }
    if (anyMapping.ifMapping.nonEmpty) {
      b.entry(
        "conditional",
        _.obj { b =>
          if (anyMapping.ifMapping.nonEmpty)
            b.entry("if", aliasFor(anyMapping.ifMapping.value()).getOrElse(""))
          if (anyMapping.thenMapping.nonEmpty)
            b.entry("then", aliasFor(anyMapping.thenMapping.value()).getOrElse(""))
          if (anyMapping.elseMapping.nonEmpty)
            b.entry("else", aliasFor(anyMapping.elseMapping.value()).getOrElse(""))
        }
      )
    }
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
