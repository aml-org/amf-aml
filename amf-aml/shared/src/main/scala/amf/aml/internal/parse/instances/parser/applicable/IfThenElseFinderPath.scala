package amf.aml.internal.parse.instances.parser.applicable

import amf.aml.client.scala.model.domain.AnyMapping
import amf.aml.internal.parse.instances.DialectInstanceContext
import NodePropertyConforms.conformsAgainstProperties
import amf.aml.internal.render.emitters.instances.DialectIndex
import amf.core.internal.parser.Root
import org.yaml.model.YMap

case class IfThenElseFinderPath(root: Root) extends FinderPath {
  override def walk(mapping: AnyMapping, map: YMap, index: DialectIndex, finder: ApplicableMappingFinder)(
      implicit ctx: DialectInstanceContext): ApplicableMapping = {
    mapping.ifMapping.option() match {
      case Some(id) =>
        val ifMapping      = index.maybeFindNodeMappingById(id).collect { case (_, mapping: AnyMapping) => mapping }.get
        val conformsWithIf = conformsAgainstProperties(map, ifMapping, root)
        val nextApplicable =
          if (conformsWithIf) mapping.thenMapping.option().toSet else mapping.elseMapping.option().toSet
        ApplicableMapping(couldFind = true, nextApplicable)
      case None =>
        // TODO: add error
        ApplicableMapping.empty
    }
  }
}
