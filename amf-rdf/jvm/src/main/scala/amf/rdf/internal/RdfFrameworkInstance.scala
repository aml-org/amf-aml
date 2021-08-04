package amf.rdf.internal

import amf.rdf.client.scala.{JenaRdfModel, RdfFramework, RdfModel}

class RdfFrameworkInstance() extends RdfFramework {

  override def emptyRdfModel(): RdfModel = new JenaRdfModel()
}
