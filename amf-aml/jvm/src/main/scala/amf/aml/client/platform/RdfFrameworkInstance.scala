package amf.aml.client.platform

import amf.core.client.scala.rdf.RdfModel
import amf.core.internal.rdf.RdfFramework
import amf.validation.internal.JenaRdfModel

import scala.scalajs.js.annotation.JSExportTopLevel

class RdfFrameworkInstance() extends RdfFramework {

  override def emptyRdfModel(): RdfModel = new JenaRdfModel()
}
