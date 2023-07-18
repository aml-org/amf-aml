package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain._
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.parse.instances.parser.ObjectCollectionPropertyParser.NodeParser
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.Root
import org.yaml.model.{YMap, YMapEntry, YNode}

object LiteralPropertyParser {
  def parse(propertyValueEntry: YMapEntry, property: PropertyLikeMapping[_], node: DialectDomainElement)(implicit
      ctx: DialectInstanceContext
  ): Unit = {
    setLiteralValue(propertyValueEntry, property, node)
  }

  private def setLiteralValue(entry: YMapEntry, property: PropertyLikeMapping[_], node: DialectDomainElement)(implicit
      ctx: DialectInstanceContext
  ): Unit = {
    val parsed = LiteralValueParser.parseLiteralValue(entry.value, property, node)(ctx.eh)
    LiteralValueSetter.setLiteralValue(parsed, entry, property, node)
  }
}

class ElementPropertyParser(private val root: Root, private val rootMap: YMap, private val nodeParser: NodeParser) {

  def parse(id: String, propertyValueEntry: YMapEntry, propertyLikeMapping: PropertyLikeMapping[_], node: DialectDomainElement)(implicit
                                                                                                                                ctx: DialectInstanceContext
  ): Unit = {
    propertyLikeMapping.classification() match {
      case ExtensionPointProperty    => parseDialectExtension(id, propertyValueEntry, propertyLikeMapping, node)
      case LiteralProperty           => LiteralPropertyParser.parse(propertyValueEntry, propertyLikeMapping, node)
      case LiteralPropertyCollection => parseLiteralCollectionProperty(id, propertyValueEntry, propertyLikeMapping, node)
      case ObjectProperty            => parseObjectProperty(id, propertyValueEntry, propertyLikeMapping, node)
      case ObjectPropertyCollection  => parseObjectCollectionProperty(id, propertyValueEntry, propertyLikeMapping, node)
      case ObjectMapProperty         => parseObjectMapProperty(id, propertyValueEntry, propertyLikeMapping, node)
      case ObjectPairProperty        => parseObjectPairProperty(id, propertyValueEntry, propertyLikeMapping, node)
      case ExternalLinkProperty      => parseExternalLinkProperty(id, propertyValueEntry, propertyLikeMapping, node)
      case _ =>
        ctx.eh.violation(DialectError, id, s"Unknown type of node property ${propertyLikeMapping.id}", propertyValueEntry.location)
    }
  }

  protected def parseDialectExtension(
      id: String,
      propertyValueEntry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement
  )(implicit ctx: DialectInstanceContext): Unit = {
    DialectExtensionParser.parse(id, propertyValueEntry, property, node, root, nodeParser)
  }

  private def parseExternalLinkProperty(
      id: String,
      propertyValueEntry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement
  )(implicit ctx: DialectInstanceContext): Unit = {
    ExternalLinkPropertyParser.parse(id, propertyValueEntry, property, node, root, parse)
  }

  protected def parseObjectUnion[T <: DomainElement](
      defaultId: String,
      path: Seq[String],
      ast: YNode,
      unionMapping: NodeWithDiscriminator[_],
      additionalProperties: Map[String, Any] = Map()
  )(implicit ctx: DialectInstanceContext): DialectDomainElement = {

    ObjectUnionParser.parse(defaultId, path, ast, unionMapping, additionalProperties, root, rootMap, parse)
  }

  protected def parseObjectProperty(
      id: String,
      propertyValueEntry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement,
      additionalProperties: Map[String, Any] = Map()
  )(implicit ctx: DialectInstanceContext): Unit = {
    ObjectPropertyParser.parse(id, propertyValueEntry, property, node, additionalProperties, parseObjectUnion, nodeParser)
  }

  protected def parseObjectMapProperty(
      id: String,
      propertyValueEntry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement,
      additionalProperties: Map[String, Any] = Map()
  )(implicit ctx: DialectInstanceContext): Unit = {
    ObjectMapPropertyParser.parse(id, propertyValueEntry, property, node, additionalProperties, parseObjectUnion, nodeParser)
  }

  protected def parseObjectPairProperty(
      id: String,
      propertyValueEntry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement
  )(implicit ctx: DialectInstanceContext): Unit =
    KeyValuePropertyParser.parse(id, propertyValueEntry, property, node)

  protected def parseObjectCollectionProperty(
      id: String,
      propertyValueEntry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement,
      additionalProperties: Map[String, Any] = Map()
  )(implicit ctx: DialectInstanceContext): Unit = {

    ObjectCollectionPropertyParser.parse(
      id,
      propertyValueEntry,
      property,
      node,
      additionalProperties,
      parseObjectUnion,
      nodeParser
    )
  }

  protected def parseLiteralCollectionProperty(
      id: String,
      propertyValueEntry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement
  )(implicit ctx: DialectInstanceContext): Unit = {
    LiteralCollectionParser.parse(propertyValueEntry, property, node)
  }
}
