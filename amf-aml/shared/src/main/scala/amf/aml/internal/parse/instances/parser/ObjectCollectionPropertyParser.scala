package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeWithDiscriminator, PropertyMapping}
import amf.aml.internal.metamodel.domain.NodeWithDiscriminatorModel
import amf.aml.internal.parse.instances.DialectInstanceParser.pathSegment
import amf.aml.internal.parse.instances.{DialectInstanceContext, NodeMappableHelper}
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.domain.DomainElement
import org.yaml.model._

import scala.collection.mutable
import scala.language.higherKinds

object ObjectCollectionPropertyParser extends NodeMappableHelper {

  type NodeParser = (String, String, YNode, NodeMappable, Map[String, Any]) => DialectDomainElement

  type ObjectUnionParser[T <: DomainElement] = (String,
                                                Seq[String],
                                                YNode,
                                                NodeWithDiscriminator[_ <: NodeWithDiscriminatorModel],
                                                Map[String, Any]) => DialectDomainElement

  def parse[T <: DomainElement](id: String,
                                propertyEntry: YMapEntry,
                                property: PropertyMapping,
                                node: DialectDomainElement,
                                additionalProperties: Map[String, Any] = Map(),
                                unionParser: ObjectUnionParser[T],
                                nodeParser: NodeParser)(implicit ctx: DialectInstanceContext): Unit = {

    // just to store Ids, and detect potentially duplicated elements in the collection
    val idsMap: mutable.Map[String, Boolean] = mutable.Map()
    val entries = propertyEntry.value.tagType match {
      case YType.Seq => propertyEntry.value.as[YSequence].nodes
      case _         => Seq(propertyEntry.value)
    }

    val elems = entries.zipWithIndex.flatMap {
      case (elementNode, nextElem) =>
        val path           = List(propertyEntry.key.as[YScalar].text, nextElem.toString)
        val nestedObjectId = pathSegment(id, path)
        property.nodesInRange match {
          case range: Seq[String] if range.size > 1 =>
            val parsed = unionParser(nestedObjectId, path, elementNode, property, additionalProperties)
            checkDuplicated(parsed, elementNode, idsMap)
            Some(parsed)
          case range: Seq[String] if range.size == 1 =>
            ctx.dialect.declares.find(_.id == range.head) match {
              case Some(nodeMapping: NodeMappable) =>
                val dialectDomainElement =
                  nodeParser(id, nestedObjectId, elementNode, nodeMapping, additionalProperties)
                checkDuplicated(dialectDomainElement, elementNode, idsMap)
                Some(dialectDomainElement)
              case _ => None
            }
          case _ => None
        }
    }
    node.withObjectCollectionProperty(property, elems, Right(propertyEntry))
  }

  def checkDuplicated(dialectDomainElement: DialectDomainElement,
                      elementNode: YNode,
                      idsMap: mutable.Map[String, Boolean])(implicit ctx: DialectInstanceContext): Unit = {
    idsMap.get(dialectDomainElement.id) match {
      case None => idsMap.update(dialectDomainElement.id, true)
      case _ =>
        ctx.eh.violation(DialectError,
                         dialectDomainElement.id,
                         s"Duplicated element in collection ${dialectDomainElement.id}",
                         elementNode.location)
    }
  }
}
