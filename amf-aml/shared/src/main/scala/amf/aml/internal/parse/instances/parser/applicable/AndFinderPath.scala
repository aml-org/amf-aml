package amf.aml.internal.parse.instances.parser.applicable

import amf.aml.client.scala.model.domain.AnyMapping
import amf.aml.internal.parse.instances.DialectInstanceContext
import ApplicableMapping.aggregate
import amf.aml.internal.render.emitters.instances.DialectIndex
import amf.core.internal.parser.Root
import org.yaml.model.YMap

object AndFinderPath extends FinderPath {
  override def walk(mapping: AnyMapping, map: YMap, index: DialectIndex, finder: ApplicableMappingFinder)(implicit
      ctx: DialectInstanceContext
  ): ApplicableMapping = {
    if (mapping.and.isEmpty) return ApplicableMapping.empty
    val andMappings      = findMappingsFor(mapping.and, index)
    val aggregateMapping = aggregate(andMappings.map(node => finder.find(map, node, index)))
    aggregateMapping
  }
}
