package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMappable, PropertyMapping, UnknownMapKeyProperty}
import amf.aml.internal.parse.instances.{DialectInstanceContext, NodeMappableHelper}
import amf.aml.internal.parse.instances.DialectInstanceParser.pathSegment
import amf.aml.internal.parse.instances.parser.ObjectCollectionPropertyParser.{NodeParser, ObjectUnionParser}
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.domain.{AmfScalar, DomainElement}
import amf.core.client.scala.vocabulary.ValueType
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Str
import amf.core.internal.parser.domain.Annotations
import org.yaml.model.{YMap, YMapEntry, YScalar, YType}

object ObjectMapPropertyParser extends NodeMappableHelper {

  def parse[T <: DomainElement](
      id: String,
      propertyEntry: YMapEntry,
      property: PropertyMapping,
      node: DialectDomainElement,
      additionalProperties: Map[String, Any] = Map(),
      unionParser: ObjectUnionParser[T],
      nodeParser: NodeParser
  )(implicit ctx: DialectInstanceContext): Unit = {
    val nested = propertyEntry.value.as[YMap].entries.map { keyEntry =>
      val path           = List(propertyEntry.key.as[YScalar].text, keyEntry.key.as[YScalar].text)
      val nestedObjectId = pathSegment(id, path)
      // we add the potential mapKey additional property
      val keyAdditionalProperties: Map[String, Any] = findHashProperties(property, keyEntry) match {
        case Some((k, v)) => additionalProperties + (k -> v)
        case _            => additionalProperties
      }
      val isUnion = (range: Seq[String]) => range.size > 1
      val parsedNode = property.nodesInRange match {
        case range: Seq[String] if isUnion(range) =>
          // we also add the potential mapValue property
          val keyValueAdditionalProperties = property.mapTermValueProperty().option() match {
            case Some(mapValueProperty) => keyAdditionalProperties + (mapValueProperty -> "")
            case _                      => keyAdditionalProperties
          }
          // now we can parse the union with all the required properties to find the right node in the union
          Some(unionParser(nestedObjectId, path, keyEntry.value, property, keyValueAdditionalProperties))
        case range: Seq[String] if range.size == 1 =>
          ctx.dialect.declares.find(_.id == range.head) match {
            case Some(nodeMapping: NodeMappable) if keyEntry.value.tagType != YType.Null =>
              Some(nodeParser(id, nestedObjectId, keyEntry.value, nodeMapping, keyAdditionalProperties, false))
            case _ => None
          }
        case _ => None
      }
      parsedNode match {
        case Some(dialectDomainElement) => Some(checkHashProperties(dialectDomainElement, property, keyEntry))
        case None                       => None
      }
    }
    node.withObjectCollectionProperty(property, nested.flatten, Right(propertyEntry))
  }

  protected def checkHashProperties(
      node: DialectDomainElement,
      propertyMapping: PropertyMapping,
      propertyEntry: YMapEntry
  )(implicit ctx: DialectInstanceContext): DialectDomainElement = {
    // TODO: check if the node already has a value and that it matches (maybe coming from a declaration)
    propertyMapping.mapTermKeyProperty().option() match {
      case Some(propId) =>
        try {
          node.set(
            Field(Str, ValueType(propId)),
            AmfScalar(propertyEntry.key.as[YScalar].text),
            Annotations(propertyEntry.key)
          )
          node.annotations.reject(_.isInstanceOf[LexicalInformation]) ++= Annotations(propertyEntry)
          node
        } catch {
          case e: UnknownMapKeyProperty =>
            ctx.eh.violation(DialectError, e.id, s"Cannot find mapping for key map property ${e.id}")
            node
        }
      case None => node
    }
  }

  protected def findHashProperties(propertyMapping: PropertyMapping, propertyEntry: YMapEntry)(implicit
      ctx: DialectInstanceContext
  ): Option[(String, Any)] = {
    propertyMapping.mapTermKeyProperty().option() match {
      case Some(propId) => Some((propId, propertyEntry.key.as[YScalar].text))
      case None         => None
    }
  }
}
