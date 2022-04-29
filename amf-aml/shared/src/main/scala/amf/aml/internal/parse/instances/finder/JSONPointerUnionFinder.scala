package amf.aml.internal.parse.instances.finder

import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping}
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.Annotations
import org.yaml.model.YMap

object JSONPointerUnionFinder {

  def find(map: YMap, allPossibleMappings: Seq[NodeMapping], id: String, root: YMap)(implicit
      ctx: DialectInstanceContext
  ): DialectDomainElement = {
    val entry   = map.key("$ref").get
    val pointer = entry.value.as[String]
    val fullPointer = if (pointer.startsWith("#")) {
      root.location + pointer
    } else {
      pointer
    }
    ctx.findJsonPointer(fullPointer) map { node =>
      if (allPossibleMappings.exists(_.id == node.definedBy.id)) {
        node
          .link(pointer, Annotations(map))
          .asInstanceOf[DialectDomainElement]
          .withId(id)
      } else {
        val linkedNode = DialectDomainElement(map).withId(id)
        linkedNode.unresolved(fullPointer, Nil, Some(map.location))
        linkedNode
      }
    } getOrElse {
      val linkedNode = DialectDomainElement(map).withId(id)
      linkedNode.unresolved(fullPointer, Nil, Some(map.location))
      linkedNode
    }
  }
}
