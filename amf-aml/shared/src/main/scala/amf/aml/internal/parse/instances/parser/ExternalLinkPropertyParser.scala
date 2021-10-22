package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping, PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.parse.instances.parser.ExternalLinkGenerator.PropertyParser
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.internal.parser.Root
import org.yaml.model.{YMap, YMapEntry, YNode, YSequence, YType}

object ExternalLinkPropertyParser {

  def parse(id: String,
            propertyEntry: YMapEntry,
            property: PropertyLikeMapping[_],
            node: DialectDomainElement,
            root: Root,
            propertyParser: PropertyParser)(implicit ctx: DialectInstanceContext): Unit = {
    // First extract the mapping information
    // External links only work over single ranges, not unions or literal ranges
    val maybeMapping: Option[NodeMapping] = property.objectRange() match {
      case range if range.length == 1 =>
        ctx.dialect.declares.find(_.id == range.head.value()) match {
          case Some(nodeMapping: NodeMapping) =>
            Some(nodeMapping)
          case _ =>
            ctx.eh
              .violation(DialectError, id, s"Cannot find object range ${range.head.value()}", propertyEntry.location)
            None
        }
      case _ =>
        ctx.eh
          .violation(DialectError,
                     id,
                     s"Individual object range required for external link property",
                     propertyEntry.location)
        None
    }

    val allowMultiple = property.allowMultiple().option().getOrElse(false)

    // now we parse the link
    maybeMapping match {
      case Some(mapping) =>
        val rangeNode = propertyEntry.value
        rangeNode.tagType match {
          case YType.Str if !allowMultiple =>
            // plain link -> we generate an anonymous node and set the id to the ref and correct type information
            generateExternalLink(id, rangeNode, mapping, root, propertyParser).foreach { elem =>
              node.withObjectField(property, elem, Right(propertyEntry))
            }
          case YType.Map if !allowMultiple => // reference
            val refMap = rangeNode.as[YMap]
            generateExternalLink(id, refMap, mapping, root, propertyParser) foreach { elem =>
              node.withObjectField(property, elem, Right(propertyEntry))
            }
          case YType.Seq if allowMultiple => // sequence of links or references
            val seq = rangeNode.as[YSequence]
            val elems = seq.nodes.flatMap { node =>
              generateExternalLink(id, node, mapping, root, propertyParser)
            }
            if (elems.nonEmpty)
              node.withObjectCollectionProperty(property, elems, Right(propertyEntry))
          case YType.Seq if !allowMultiple => // error
            ctx.eh.violation(DialectError,
                             id,
                             s"AllowMultiple not enable, sequence of external links not supported",
                             propertyEntry.location)
          case _ =>
            ctx.eh.violation(DialectError, id, s"Not supported external link range", propertyEntry.location)
        }
      case _ => // ignore
    }
  }

  private def generateExternalLink(
      id: String,
      node: YNode,
      mapping: NodeMapping,
      root: Root,
      parseProperty: PropertyParser)(implicit ctx: DialectInstanceContext): Option[DialectDomainElement] = {
    ExternalLinkGenerator.generate(id, node, mapping, root, parseProperty)
  }
}
