package amf.plugins.document.vocabularies.parser.instances

import amf.core.model.DataType
import amf.core.model.domain.{AmfScalar, DomainElement}
import amf.core.parser.{Annotations, ParserContext, YMapOps}
import amf.core.vocabulary.Namespace
import amf.plugins.document.vocabularies.model.domain.{LiteralProperty, NodeMapping, PropertyMapping}
import amf.plugins.document.vocabularies.parser.common.{AnnotationsParser, SyntaxErrorReporter}
import amf.validation.DialectValidations.DialectError
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model._

class InstanceNPparser(element:DomainElement, nodeMap:YMap, mapping:NodeMapping)(implicit ctx:ParserContext with SyntaxErrorReporter) extends AnnotationsParser{

  def parse() = {
    element.annotations ++= Annotations(nodeMap)

    //annotations?
    //parseAnnotations(nodeMap, nodeMap, ctx.declarations)

    mapping.propertiesMapping().foreach { propertyMapping =>
      val propertyName = propertyMapping.name().value()
      nodeMap.key(propertyName) match {
        case Some(entry) =>
          parseProperty(element.id, entry, propertyMapping, element)
        case None => // ignore
      }
    }
    checkClosedNode(element.id, mapping.id, nodeMap.map, mapping, nodeMap, false,None )
    element

  }

  protected def parseProperty(id: String,
                              propertyEntry: YMapEntry,
                              property: PropertyMapping,
                              node: DomainElement): Unit = {
    property.classification() match {
//      case ExtensionPointProperty    => parseDialectExtension(id, propertyEntry, property, node)
      case LiteralProperty           => parseLiteralProperty(id, propertyEntry, property, node)
//      case LiteralPropertyCollection => parseLiteralCollectionProperty(id, propertyEntry, property, node)
//      case ObjectProperty            => parseObjectProperty(id, propertyEntry, property, node)
//      case ObjectPropertyCollection  => parseObjectCollectionProperty(id, propertyEntry, property, node)
//      case ObjectMapProperty         => parseObjectMapProperty(id, propertyEntry, property, node)
//      case ObjectPairProperty        => parseObjectPairProperty(id, propertyEntry, property, node)
//      case ExternalLinkProperty      => parseExternalLinkProperty(id, propertyEntry, property, node)
      case _ =>
        ctx.eh.violation(DialectError, id, s"Unknown type of node property ${property.id}", propertyEntry)
    }
  }

  protected def setLiteralValue(entry: YMapEntry, property: PropertyMapping, node: DomainElement): Unit = {
    parseLiteralValue(entry.value, property, node) match {
      case Some(b: Boolean)          => setProperty(property, node, b, entry)
      case Some(i: Int)              => setProperty(property, node, i, entry)
      case Some(f: Float)            => setProperty(property, node, f, entry)
      case Some(d: Double)           => setProperty(property, node, d, entry)
      case Some(s: String)           => setProperty(property, node, s, entry)
      case Some(("link", l: String)) => setProperty(property, node, l, entry)
      case Some(d: SimpleDateTime)   => setProperty(property, node, d, entry)
      case _                         => setProperty(property, node, entry.value,entry)
    }
  }

  private def setProperty(property: PropertyMapping, node:DomainElement, value:Any, entry:YMapEntry) ={
    node.set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
  }

  protected def parseLiteralProperty(id: String,
                                     propertyEntry: YMapEntry,
                                     property: PropertyMapping,
                                     node: DomainElement): Unit = {
    setLiteralValue(propertyEntry, property, node)
  }

  protected def parseLiteralValue(value: YNode, property: PropertyMapping, node: DomainElement): Option[_] = {

    value.tagType match {
      case YType.Bool
        if (property.literalRange().value() == DataType.Boolean) || property
          .literalRange()
          .value() == DataType.Any =>
        Some(value.as[Boolean])
      case YType.Bool =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
          property,
          property.literalRange().value(),
          DataType.Boolean,
          value)
        None
      case YType.Int
        if property.literalRange().value() == DataType.Integer || property
          .literalRange()
          .value() == DataType.Number || property.literalRange().value() == DataType.Any =>
        Some(value.as[Int])
      case YType.Int
        if property.literalRange().value() == DataType.Float ||
          property.literalRange().value() == DataType.Double =>
        Some(value.as[Double])
      case YType.Int =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
          property,
          property.literalRange().value(),
          DataType.Integer,
          value)
        None
      case YType.Str
        if property.literalRange().value() == DataType.String || property.literalRange().value() == DataType.Any =>
        Some(value.as[YScalar].text)
      case YType.Str if property.literalRange().value() == DataType.AnyUri =>
        Some(value.as[YScalar].text)
      case YType.Str if property.literalRange().value() == (Namespace.Shapes + "link").iri() =>
        Some(("link", value.as[YScalar].text))
      case YType.Str
        if property.literalRange().value() == DataType.Time ||
          property.literalRange().value() == DataType.Date ||
          property.literalRange().value() == DataType.DateTime =>
        Some(YNode(value.value, YType.Timestamp).as[SimpleDateTime])
      case YType.Str if property.literalRange().value() == (Namespace.Shapes + "guid").iri() =>
        Some(value.as[YScalar].text)
      case YType.Str =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
          property,
          property.literalRange().value(),
          DataType.String,
          value)
        None
      case YType.Float
        if property.literalRange().value() == DataType.Float ||
          property.literalRange().value() == DataType.Number ||
          property.literalRange().value() == DataType.Double ||
          property.literalRange().value() == DataType.Any =>
        Some(value.as[Double])
      case YType.Float =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
          property,
          property.literalRange().value(),
          DataType.Float,
          value)
        None

      case YType.Timestamp
        if property.literalRange().value() == DataType.Time ||
          property.literalRange().value() == DataType.Date ||
          property.literalRange().value() == DataType.DateTime ||
          property.literalRange().value() == DataType.Any =>
        Some(value.as[SimpleDateTime])

      case YType.Timestamp if property.literalRange().value() == DataType.String =>
        Some(value.as[YScalar].text)

      case YType.Timestamp =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
          property,
          property.literalRange().value(),
          DataType.DateTime,
          value)
        Some(value.as[String])

      case YType.Null =>
        None
      case _ =>
        ctx.eh.violation(DialectError, node.id, s"Unsupported scalar type ${value.tagType}", value)
        Some(value.as[String])
    }
  }

  def checkClosedNode(id: String,
                      nodetype: String,
                      entries: Map[YNode, YNode],
                      mapping: NodeMapping,
                      ast: YPart,
                      rootNode: Boolean,
                      additionalKey: Option[String]): Unit = {

    val props = mapping.propertiesMapping().map(_.name().value()).toSet
    val inNode = entries.keys
      .map(_.value.asInstanceOf[YScalar].text)
      .filter(p => !p.startsWith("$") && !p.startsWith("(") && !p.startsWith("x-"))
      .filterNot(additionalKey.contains)
      .toSet
    val outside = inNode.diff(props)
    if (outside.nonEmpty) {
      outside.foreach { prop =>
        val posAst = entries.find(_._1.toString == prop).map(_._2).getOrElse(ast)
        ctx.closedNodeViolation(id, prop, nodetype, posAst)
      }
    }
  }
}
