package amf.rdf.client.platform

import amf.rdf.internal.RdfFramework

object RdfFrameworkBuilder {

  def build(): RdfFramework = new RdfFrameworkInstance()
}
