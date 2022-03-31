package amf.aml.internal.render.emitters.instances

import amf.aml.client.scala.model.document.{Dialect, DialectLibrary}
import amf.aml.client.scala.model.domain.NodeMappable.AnyNodeMappable
import amf.aml.client.scala.model.domain.{ConditionalNodeMapping, NodeMapping, UnionNodeMapping}
import amf.aml.internal.render.emitters.instances.DialectIndex.NodeMappingId

import scala.collection.mutable

object DialectIndex {
  type NodeMappingId = String
  def apply(dialect: Dialect, finder: NodeMappableFinder): DialectIndex = new DialectIndex(dialect, finder)
}

class DialectIndex(private val dialect: Dialect, private val finder: NodeMappableFinder) {
  val cache: mutable.HashMap[NodeMappingId, (Dialect, AnyNodeMappable)] = mutable.HashMap.empty

  def findNodeMappingById(nodeMappingId: NodeMappingId): (Dialect, AnyNodeMappable) = {
    cache
      .get(nodeMappingId)
      .orElse(maybeFindNodeMappingById(nodeMappingId)) match {
      case Some(result) =>
        cache(nodeMappingId) = result
        result
      case None =>
        throw new Exception(s"Cannot find node mapping $nodeMappingId")
    }
  }

  def findAllNodeMappings(mappableId: String): Seq[NodeMapping] = {
    findNodeMappingById(mappableId) match {
      case (_, nodeMapping: NodeMapping) => Seq(nodeMapping)
      case (_, unionMapping: UnionNodeMapping) =>
        val mappables = unionMapping.objectRange() map { rangeId =>
          findNodeMappingById(rangeId.value())._2
        }
        mappables.collect { case nodeMapping: NodeMapping => nodeMapping }
      case (_, conditionalMapping: ConditionalNodeMapping) =>
        val mappables = Seq(conditionalMapping.ifMapping.value(),
                            conditionalMapping.thenMapping.value(),
                            conditionalMapping.thenMapping.value()).map(id => findNodeMappingById(id)._2)
        mappables.collect { case nodeMapping: NodeMapping => nodeMapping }
      case _ => Nil
    }
  }

  def maybeFindNodeMappingById(nodeMappingId: NodeMappingId): Option[(Dialect, AnyNodeMappable)] = {
    dialect.declares
      .find { element =>
        element.id == nodeMappingId
      }
      .collect {
        case mappable: AnyNodeMappable => (dialect, mappable)
      } orElse {
      dialect.references
        .collect {
          case lib: DialectLibrary => lib.declares.find(_.id == nodeMappingId)
        }
        .collectFirst {
          case Some(mapping: AnyNodeMappable) => (dialect, mapping)
        }
    } orElse findNodeInRegistry(nodeMappingId)
  }

  private def findNodeInRegistry(nodeMappingId: String): Option[(Dialect, AnyNodeMappable)] =
    finder.findNode(nodeMappingId)
}
