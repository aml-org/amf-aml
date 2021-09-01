package amf.rdf.internal.parsers

import amf.core.client.scala.model.domain.{ArrayNode, DataNode}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.domain.Annotations
import amf.rdf.client.scala.{PropertyObject, Uri}
import amf.rdf.internal.graph.NodeFinder
import amf.rdf.internal.{RdfParserCommon, RdfParserContext}

class DynamicArrayParser(linkFinder: NodeFinder, sourcesRetriever: SourcesRetriever)(
    implicit val ctx: RdfParserContext)
    extends RdfParserCommon {
  def parse(propertyObject: PropertyObject): ArrayNode = {
    val nodeAnnotations = linkFinder.findLink(propertyObject) match {
      case Some(node) =>
        val sources = sourcesRetriever.retrieve(node)
        annots(sources, node.subject)
      case None => Annotations()
    }
    val nodes = parseDynamicArrayInner(propertyObject)
    val array = ArrayNode(nodeAnnotations)
    nodes.foreach { array.addMember }
    array
  }

  private def parseDynamicArrayInner(entry: PropertyObject, acc: Seq[DataNode] = Nil): Seq[DataNode] = {
    linkFinder.findLink(entry) match {
      case Some(n) =>
        val nextNode  = n.getProperties((Namespace.Rdf + "next").iri()).getOrElse(Nil).headOption
        val firstNode = n.getProperties((Namespace.Rdf + "first").iri()).getOrElse(Nil).headOption
        val updatedAcc = firstNode match {
          case Some(id @ Uri(_)) =>
            new DynamicTypeParser(linkFinder, sourcesRetriever).parse(id) match {
              case Some(member) => acc ++ Seq(member)
              case _            => acc
            }
          case _ => acc
        }
        nextNode match {
          case Some(nextNodeProp @ Uri(id)) if id != (Namespace.Rdf + "nil").iri() =>
            parseDynamicArrayInner(nextNodeProp, updatedAcc)
          case _ =>
            updatedAcc
        }
      case None => acc
    }
  }
}
