package amf.rdf.internal

import amf.rdf.client.scala.{RdfFramework, RdfModel, RdflibRdfModel}

import scala.scalajs.js.annotation.JSExportTopLevel

class RdfFrameworkInstance() extends RdfFramework {
  override def emptyRdfModel(): RdfModel = new RdflibRdfModel()
}
