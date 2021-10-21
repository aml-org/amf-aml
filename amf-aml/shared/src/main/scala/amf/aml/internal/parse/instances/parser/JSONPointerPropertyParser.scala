package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.internal.parser.{Root, YMapOps}
import amf.core.internal.parser.domain.Annotations
import org.yaml.model.YMap

object JSONPointerPropertyParser {

  def parse(map: YMap, mapping: PropertyLikeMapping[_], id: String, node: DialectDomainElement, root: Root)(
      implicit ctx: DialectInstanceContext): Unit = {
    val entry   = map.key("$ref").get
    val pointer = entry.value.as[String]
    val fullPointer = if (pointer.startsWith("#")) {
      root.location + pointer
    } else {
      pointer
    }

    ctx.findJsonPointer(fullPointer) map { node =>
      node
        .link(pointer, Annotations(map))
        .asInstanceOf[DialectDomainElement]
        .withId(id)
    } match {
      case Some(s) =>
        ctx.nodeMappableFinder.findNode(s.definedBy.id) match {
          case Some((dialect, _)) =>
            ctx.nestedDialects ++= Seq(dialect)
            val linkedExternal = s
              .link(pointer, Annotations(map))
              .asInstanceOf[DialectDomainElement]
              .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            node.withObjectField(mapping, linkedExternal, Right(entry))
          case None =>
            ctx.eh.violation(DialectError,
                             id,
                             s"Cannot find dialect for anyNode node mapping ${s.definedBy.id}",
                             map.location)
        }
      case None =>
        ctx.eh.violation(
            DialectError,
            id,
            s"anyNode reference must be to a known node or an external fragment, unknown JSON Pointer: '$pointer'",
            map.location
        )
    }
  }
}
