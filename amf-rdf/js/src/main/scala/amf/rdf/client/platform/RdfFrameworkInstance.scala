package amf.rdf.client.platform

import amf.rdf.internal.{RdfFramework, RdfModel}

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("RdfFrameworkInstance")
class RdfFrameworkInstance() extends RdfFramework {

  override def emptyRdfModel(): RdfModel = new RdflibRdfModel()
}
