package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.parse.instances.DialectInstanceParser.pathSegment
import amf.aml.internal.parse.instances.parser.ObjectCollectionPropertyParser.{NodeParser, ObjectUnionParser}
import amf.aml.internal.parse.instances.{DialectInstanceContext, NodeMappableHelper}
import amf.core.client.scala.model.domain.DomainElement
import org.yaml.model.{YMapEntry, YScalar}

object ObjectPropertyParser extends NodeMappableHelper {

  def parse[T <: DomainElement](id: String,
                                propertyEntry: YMapEntry,
                                property: PropertyLikeMapping[_],
                                node: DialectDomainElement,
                                additionalProperties: Map[String, Any] = Map(),
                                unionParser: ObjectUnionParser[T],
                                nodeParser: NodeParser)(implicit ctx: DialectInstanceContext): Unit = {
    val path           = propertyEntry.key.as[YScalar].text
    val nestedObjectId = pathSegment(id, List(path))
    property.nodesInRange match {
      case range: Seq[String] if range.size > 1 =>
        val parsedRange =
          unionParser(nestedObjectId, Seq(path), propertyEntry.value, property, additionalProperties)
        node.withObjectField(property, parsedRange, Right(propertyEntry))
      case range: Seq[String] if range.size == 1 =>
        ctx.dialect.declares.find(_.id == range.head) match {
          case Some(nodeMapping: NodeMappable) =>
            val dialectDomainElement =
              nodeParser(id, nestedObjectId, propertyEntry.value, nodeMapping, additionalProperties)
            node.withObjectField(property, dialectDomainElement, Right(propertyEntry))
          case _ => // ignore
        }
      case _ => // TODO: throw exception, illegal range
    }
  }
}
