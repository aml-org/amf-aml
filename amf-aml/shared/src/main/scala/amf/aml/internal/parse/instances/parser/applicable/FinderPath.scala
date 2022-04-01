package amf.aml.internal.parse.instances.parser.applicable

import amf.aml.client.scala.model.domain.AnyMapping
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.render.emitters.instances.DialectIndex
import amf.core.client.scala.model.StrField
import amf.core.internal.parser.Root
import org.yaml.model.YMap

trait FinderPath {

  def walk(mapping: AnyMapping, map: YMap, index: DialectIndex, finder: ApplicableMappingFinder)(
      implicit ctx: DialectInstanceContext): ApplicableMapping

  protected def findMappingsFor(ids: Seq[StrField], index: DialectIndex)(
      implicit ctx: DialectInstanceContext): Seq[AnyMapping] = {
    val mappings = ids
      .flatMap(_.option())
      .flatMap(index.maybeFindNodeMappingById)
      .collect { case (_, mapping: AnyMapping) => mapping }
    val foundAllMappings = mappings.size == ids.size
    if (!foundAllMappings) {
      // TODO: throw error if for a component, there aren't any suitable mappings
    }
    mappings
  }
}
