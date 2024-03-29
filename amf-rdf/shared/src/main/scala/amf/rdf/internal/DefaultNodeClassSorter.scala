package amf.rdf.internal

import amf.core.client.scala.vocabulary.Namespace
import amf.rdf.client.scala.Node

class DefaultNodeClassSorter() {

  private val deferredTypesSet = Set(
    (Namespace.Document + "Document").iri(),
    (Namespace.Document + "Fragment").iri(),
    (Namespace.Document + "Module").iri(),
    (Namespace.Document + "Unit").iri(),
    (Namespace.Shacl + "Shape").iri(),
    (Namespace.Shapes + "Shape").iri(),
    (Namespace.ApiContract + "Message").iri(),
    (Namespace.Core + "Operation").iri(),
    (Namespace.Core + "Parameter").iri(),
    (Namespace.Core + "Payload").iri(),
    (Namespace.Core + "Request").iri(),
    (Namespace.Core + "Response").iri()
  )

  def sortedClassesOf(node: Node): Seq[String] = {
    node.classes.partition(deferredTypesSet.contains) match {
      case (deferred, others) => others ++ deferred.sorted // we just use the fact that lexical order is correct
    }
  }
}
