package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{NodeMapping, UnionNodeMapping}
import amf.aml.internal.utils.AmlExtensionSyntax.NodeMappable

object AmlSubGraphCollector {
  def collect(sourceId: String, dialect: Dialect): Set[String] = {
    val acc = dialect.declares.collect {
      case element: NodeMapping      => (element.id -> element)
      case element: UnionNodeMapping => (element.id -> element)
    }.toMap
    collect(sourceId, acc, Set(sourceId))
  }

  private def collect(current: String,
                      mappingMap: Map[String, NodeMappable],
                      seen: Set[String] = Set.empty): Set[String] = {
    mappingMap
      .get(current)
      .map { mapping =>
        val toCheck    = ranges(mapping)
        val toTraverse = toCheck.diff(seen)
        val nextSeen   = seen ++ toTraverse
        toTraverse.foldLeft(nextSeen) { (nextSeen, toSee) =>
          nextSeen ++ collect(toSee, mappingMap, nextSeen)
        }
      }
      .getOrElse(Set.empty)
  }

  private def ranges(mapping: NodeMappable): Set[String] = mapping match {
    case m: NodeMapping      => m.propertiesMapping().flatMap(_.objectRange()).flatMap(_.option()).toSet
    case m: UnionNodeMapping => m.objectRange().flatMap(_.option()).toSet
    case _                   => Set.empty
  }
}
