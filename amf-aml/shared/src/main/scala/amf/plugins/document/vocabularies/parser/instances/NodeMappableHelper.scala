package amf.plugins.document.vocabularies.parser.instances

import amf.plugins.document.vocabularies.model.domain.{NodeMappable, NodeMapping, UnionNodeMapping}

trait NodeMappableHelper {

  def allNodeMappingIds(mapping: NodeMappable): Set[String] = mapping match {
    case nodeMapping: NodeMapping           => Set(nodeMapping.id)
    case unionNodeMapping: UnionNodeMapping => unionNodeMapping.objectRange().map(_.value()).toSet
  }
}
