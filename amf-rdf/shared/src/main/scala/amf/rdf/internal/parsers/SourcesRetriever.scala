package amf.rdf.internal.parsers

import amf.core.client.scala.model.document.SourceMap
import amf.core.internal.metamodel.domain.DomainElementModel
import amf.rdf.client.scala.Node
import amf.rdf.internal.graph.NodeFinder

class SourcesRetriever(linkFinder: NodeFinder) {
  def retrieve(node: Node): SourceMap = {
    node
      .getProperties(DomainElementModel.Sources.value.iri())
      .flatMap { properties =>
        if (properties.nonEmpty) {
          linkFinder.findLink(properties.head) match {
            case Some(sourceNode) => Some(new SourceNodeParser(linkFinder).parse(sourceNode))
            case _                => None
          }
        } else {
          None
        }
      }
      .getOrElse(SourceMap.empty)
  }
}
