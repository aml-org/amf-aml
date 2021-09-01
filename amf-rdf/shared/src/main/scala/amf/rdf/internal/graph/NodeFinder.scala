package amf.rdf.internal.graph

import amf.rdf.client.scala.{Node, PropertyObject, RdfModel, Uri}

class NodeFinder(graph: RdfModel) {
  def findLink(property: PropertyObject): Option[Node] = {
    property match {
      case Uri(v) => graph.findNode(v)
      case _      => None
    }
  }
}
