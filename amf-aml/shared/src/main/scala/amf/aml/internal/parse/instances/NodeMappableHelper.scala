package amf.aml.internal.parse.instances

import amf.aml.client.scala.model.domain.{ConditionalNodeMapping, NodeMappable, NodeMapping, UnionNodeMapping}

trait NodeMappableHelper {
  type NodeMappable = NodeMappable.AnyNodeMappable
  def allNodeMappingIds(mapping: NodeMappable): Set[String] = mapping match {
    case nodeMapping: NodeMapping           => Set(nodeMapping.id)
    case unionNodeMapping: UnionNodeMapping => unionNodeMapping.objectRange().map(_.value()).toSet
    case conditionalNodeMapping: ConditionalNodeMapping =>
      Set(conditionalNodeMapping.ifMapping.value(),
          conditionalNodeMapping.thenMapping.value(),
          conditionalNodeMapping.elseMapping.value())
  }
}
