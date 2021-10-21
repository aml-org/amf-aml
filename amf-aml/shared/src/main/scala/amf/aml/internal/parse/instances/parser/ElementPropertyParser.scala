package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain._
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.parse.instances.parser.ObjectCollectionPropertyParser.NodeParser
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.Root
import org.yaml.model.{YMap, YMapEntry, YNode}

class ElementPropertyParser(private val root: Root, private val rootMap: YMap, private val nodeParser: NodeParser) {

  def parse(id: String, propertyEntry: YMapEntry, property: PropertyLikeMapping[_], node: DialectDomainElement)(
      implicit ctx: DialectInstanceContext): Unit = {
    property.classification() match {
      case ExtensionPointProperty    => parseDialectExtension(id, propertyEntry, property, node)
      case LiteralProperty           => parseLiteralProperty(id, propertyEntry, property, node)
      case LiteralPropertyCollection => parseLiteralCollectionProperty(id, propertyEntry, property, node)
      case ObjectProperty            => parseObjectProperty(id, propertyEntry, property, node)
      case ObjectPropertyCollection  => parseObjectCollectionProperty(id, propertyEntry, property, node)
      case ObjectMapProperty if property.isInstanceOf[PropertyMapping] =>
        parseObjectMapProperty(id, propertyEntry, property.asInstanceOf[PropertyMapping], node)
      case ObjectPairProperty => parseObjectPairProperty(id, propertyEntry, property, node)
      case ExternalLinkProperty =>
        parseExternalLinkProperty(id, propertyEntry, property, node)
      case _ =>
        ctx.eh.violation(DialectError, id, s"Unknown type of node property ${property.id}", propertyEntry.location)
    }
  }

  protected def parseDialectExtension(id: String,
                                      propertyEntry: YMapEntry,
                                      property: PropertyLikeMapping[_],
                                      node: DialectDomainElement)(implicit ctx: DialectInstanceContext): Unit = {
    DialectExtensionParser.parse(id, propertyEntry, property, node, root, nodeParser)
  }

  private def parseExternalLinkProperty(id: String,
                                        propertyEntry: YMapEntry,
                                        property: PropertyLikeMapping[_],
                                        node: DialectDomainElement)(implicit ctx: DialectInstanceContext): Unit = {
    ExternalLinkPropertyParser.parse(id, propertyEntry, property, node, root, parse)
  }

  protected def parseObjectUnion[T <: DomainElement](
      defaultId: String,
      path: Seq[String],
      ast: YNode,
      unionMapping: NodeWithDiscriminator[_],
      additionalProperties: Map[String, Any] = Map())(implicit ctx: DialectInstanceContext): DialectDomainElement = {

    ObjectUnionParser.parse(defaultId, path, ast, unionMapping, additionalProperties, root, rootMap, parse)
  }

  protected def parseObjectProperty(
      id: String,
      propertyEntry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement,
      additionalProperties: Map[String, Any] = Map())(implicit ctx: DialectInstanceContext): Unit = {
    ObjectPropertyParser.parse(id, propertyEntry, property, node, additionalProperties, parseObjectUnion, nodeParser)
  }

  protected def parseObjectMapProperty(
      id: String,
      propertyEntry: YMapEntry,
      property: PropertyMapping,
      node: DialectDomainElement,
      additionalProperties: Map[String, Any] = Map())(implicit ctx: DialectInstanceContext): Unit = {
    ObjectMapPropertyParser.parse(id,
                                  propertyEntry,
                                  property,
                                  node,
                                  additionalProperties,
                                  parseObjectUnion,
                                  nodeParser)
  }

  protected def parseObjectPairProperty(id: String,
                                        propertyEntry: YMapEntry,
                                        property: PropertyLikeMapping[_],
                                        node: DialectDomainElement)(implicit ctx: DialectInstanceContext): Unit =
    KeyValuePropertyParser.parse(id, propertyEntry, property, node)

  protected def parseObjectCollectionProperty(
      id: String,
      propertyEntry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement,
      additionalProperties: Map[String, Any] = Map())(implicit ctx: DialectInstanceContext): Unit = {

    ObjectCollectionPropertyParser.parse(id,
                                         propertyEntry,
                                         property,
                                         node,
                                         additionalProperties,
                                         parseObjectUnion,
                                         nodeParser)
  }

  protected def parseLiteralValue(value: YNode, property: PropertyLikeMapping[_], node: DialectDomainElement)(
      implicit ctx: DialectInstanceContext): Option[_] = {

    LiteralValueParser.parseLiteralValue(value, property, node)
  }

  // TODO: This should receive annotations instead of an entry. Unrelated concepts in the same method
  protected def setLiteralValue(entry: YMapEntry, property: PropertyLikeMapping[_], node: DialectDomainElement)(
      implicit ctx: DialectInstanceContext): Unit = {
    val parsed = parseLiteralValue(entry.value, property, node)
    LiteralValueSetter.setLiteralValue(parsed, entry, property, node)
  }

  protected def parseLiteralProperty(id: String,
                                     propertyEntry: YMapEntry,
                                     property: PropertyLikeMapping[_],
                                     node: DialectDomainElement)(implicit ctx: DialectInstanceContext): Unit = {
    setLiteralValue(propertyEntry, property, node)
  }

  protected def parseLiteralCollectionProperty(
      id: String,
      propertyEntry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement)(implicit ctx: DialectInstanceContext): Unit = {
    LiteralCollectionParser.parse(propertyEntry, property, node)
  }
}
