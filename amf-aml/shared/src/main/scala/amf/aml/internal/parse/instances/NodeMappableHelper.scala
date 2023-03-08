package amf.aml.internal.parse.instances

import amf.aml.client.scala.model.domain.{AnyMapping, NodeMappable, NodeMapping, UnionNodeMapping}

trait NodeMappableHelper {

  type NodeMappable = NodeMappable.AnyNodeMappable

  def allNodeMappingIds(mapping: NodeMappable): Set[String] = mapping match {
    case nodeMapping: NodeMapping           => nodeMappingMappables(nodeMapping)
    case unionNodeMapping: UnionNodeMapping => unionMappingMappables(unionNodeMapping)
  }

  private def anyMappingMappables(anyMapping: AnyMapping): Set[String] =
    (anyMapping.and.map(_.value()) ++ anyMapping.or.map(_.value()) ++ anyMapping.components
      .map(_.value())).toSet ++ Set(
      anyMapping.ifMapping.value(),
      anyMapping.thenMapping.value(),
      anyMapping.elseMapping.value()
    )

  private def nodeMappingMappables(nodeMapping: NodeMapping): Set[String] =
    anyMappingMappables(nodeMapping) ++ Set(nodeMapping.id)

  private def unionMappingMappables(unionNodeMapping: UnionNodeMapping): Set[String] =
    anyMappingMappables(unionNodeMapping) ++ unionNodeMapping.objectRange().map(_.value()).toSet

}
