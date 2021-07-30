package amf.rdf.internal.graph

import amf.rdf.internal.{PropertyObject, RdfModel, Uri}

class NodeFinder(graph: RdfModel) {
  def findLink(property: PropertyObject) = {
    property match {
      case Uri(v) => graph.findNode(v)
      case _      => None
    }
  }
}
