package amf.aml.internal.parse.instances

import amf.aml.client.scala.model.domain.{
  AnyMapping,
  ConditionalNodeMapping,
  NodeMappable,
  NodeMapping,
  UnionNodeMapping
}

trait NodeMappableHelper {

  type NodeMappable = NodeMappable.AnyNodeMappable

  def allNodeMappingIds(mapping: NodeMappable): Set[String] = mapping match {
    case nodeMapping: NodeMapping                       => nodeMappingMappables(nodeMapping)
    case unionNodeMapping: UnionNodeMapping             => unionMappingMappables(unionNodeMapping)
    case conditionalNodeMapping: ConditionalNodeMapping => conditionalMappingMappables(conditionalNodeMapping)
  }

  private def anyMappingMappables(anyMapping: AnyMapping): Set[String] =
    (anyMapping.and.map(_.value()) ++ anyMapping.or.map(_.value()) ++ anyMapping.components.map(_.value())).toSet

  private def nodeMappingMappables(nodeMapping: NodeMapping): Set[String] =
    anyMappingMappables(nodeMapping) ++ Set(nodeMapping.id)

  private def unionMappingMappables(unionNodeMapping: UnionNodeMapping): Set[String] =
    anyMappingMappables(unionNodeMapping) ++ unionNodeMapping.objectRange().map(_.value()).toSet

  private def conditionalMappingMappables(conditionalNodeMapping: ConditionalNodeMapping): Set[String] =
    anyMappingMappables(conditionalNodeMapping) ++ Set(conditionalNodeMapping.ifMapping.value(),
                                                       conditionalNodeMapping.thenMapping.value(),
                                                       conditionalNodeMapping.elseMapping.value())
}
