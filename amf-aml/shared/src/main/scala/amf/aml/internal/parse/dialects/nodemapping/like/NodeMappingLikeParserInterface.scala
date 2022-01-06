package amf.aml.internal.parse.dialects.nodemapping.like

import amf.aml.client.scala.model.domain.NodeMappable.AnyNodeMappable
import amf.aml.internal.parse.dialects.DialectContext
import amf.core.client.scala.model.domain.DomainElement
import org.yaml.model.YMap

abstract class NodeMappingLikeParserInterface(implicit ctx: DialectContext) {

  def parse(map: YMap, adopt: DomainElement => Any, isFragment: Boolean = false): AnyNodeMappable

}
