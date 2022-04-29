package amf.aml.internal.parse.instances.parser.applicable

import amf.aml.client.scala.model.domain.AnyMapping
import amf.aml.internal.parse.instances.DialectInstanceContext
import ApplicableMapping.invalid
import NodePropertyConforms.conformsAgainstProperties
import amf.aml.internal.render.emitters.instances.DialectIndex
import amf.core.internal.parser.Root
import org.yaml.model.YMap

case class OrFinderPath(root: Root) extends FinderPath {

  override def walk(mapping: AnyMapping, map: YMap, index: DialectIndex, finder: ApplicableMappingFinder)(implicit
      ctx: DialectInstanceContext
  ): ApplicableMapping = {
    if (mapping.or.isEmpty) return ApplicableMapping.empty
    val orMappings = findMappingsFor(mapping.or, index)
    val applicableList = orMappings.map { mapping =>
      findMatches(map, index, mapping, finder)
    }
    val onlyOneMappingConforms = applicableList.count(_.couldFind) == 1
    if (!onlyOneMappingConforms) {
      // TODO: throw error
    }
    applicableList.find(_.couldFind).getOrElse(invalid)
  }

  protected def findMatches(map: YMap, index: DialectIndex, orMap: AnyMapping, finder: ApplicableMappingFinder)(implicit
      ctx: DialectInstanceContext
  ): ApplicableMapping = {
    val applies = conformsAgainstProperties(map, orMap, root)
    if (!applies) ApplicableMapping(couldFind = false, Set.empty[String])
    else {
      val nested = finder.find(map, orMap, index)
      nested
    }
  }
}
