package amf.rdf.client.platform

import amf.rdf.internal.{RdfFramework, RdfModel}

class RdfFrameworkInstance() extends RdfFramework {

  override def emptyRdfModel(): RdfModel = new JenaRdfModel()
}
