package amf.aml.internal.parse.instances.parser.applicable

import amf.aml.client.scala.model.domain.{AnyMapping, NodeMapping}
import amf.aml.internal.parse.instances.DialectInstanceContext
import NodePropertyConforms.conformsAgainstProperties
import amf.aml.internal.render.emitters.instances.{DefaultNodeMappableFinder, DialectIndex}
import amf.core.internal.parser.Root
import org.yaml.model.YMap

import scala.language.higherKinds

case class ApplicableMappingFinder(private val root: Root) {

  val paths = List(
    AndFinderPath,
    OrFinderPath(root),
    IfThenElseFinderPath(root)
  )

  def find(map: YMap, mapping: AnyMapping)(implicit ctx: DialectInstanceContext): Option[NodeMapping] = {
    val index                     = DialectIndex(ctx.dialect, DefaultNodeMappableFinder(ctx.dialect))
    val ApplicableMapping(_, ids) = find(map, mapping, index)
    val isMappingCombination      = ids.size != 1
    if (isMappingCombination) index.findCompositeMapping(ids).collect { case mapping: NodeMapping => mapping }
    else
      index.maybeFindNodeMappingById(ids.head).collect { case (_, mapping: NodeMapping) =>
        mapping
      }

  }

  private[parser] def find(map: YMap, mapping: AnyMapping, index: DialectIndex)(implicit
      ctx: DialectInstanceContext
  ): ApplicableMapping = {
    val appliesToCurrent =
      if (conformsAgainstProperties(map, mapping, root)) ApplicableMapping(couldFind = true, Set(mapping.id))
      else ApplicableMapping(couldFind = true, Set.empty[String])
    paths.foldLeft(appliesToCurrent) { (applicable, path) =>
      applicable.add(path.walk(mapping, map, index, this))
    }
  }
}

object ApplicableMapping {
  val invalid: ApplicableMapping = ApplicableMapping(couldFind = false, Set.empty)
  val empty: ApplicableMapping   = ApplicableMapping(couldFind = true, Set.empty)

  def aggregate(mappings: Seq[ApplicableMapping]): ApplicableMapping = {
    mappings.foldLeft(empty) { (acc, curr) =>
      acc.add(curr)
    }
  }
}

case class ApplicableMapping(couldFind: Boolean, mappings: Set[String]) {
  def add(applicable: ApplicableMapping): ApplicableMapping =
    copy(couldFind = this.couldFind && applicable.couldFind, this.mappings ++ applicable.mappings)
}
