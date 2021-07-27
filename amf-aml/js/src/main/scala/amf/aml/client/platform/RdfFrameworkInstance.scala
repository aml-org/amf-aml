package amf.aml.client.platform

import amf.core.client.scala.rdf.RdfModel
import amf.core.internal.rdf.RdfFramework
import amf.validation.internal.RdflibRdfModel

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("RdfFrameworkInstance")
class RdfFrameworkInstance() extends RdfFramework {

  override def emptyRdfModel(): RdfModel = new RdflibRdfModel()
}
