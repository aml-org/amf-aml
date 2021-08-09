package amf.rdf.internal

import amf.rdf.client.scala.RdfFramework

object RdfFrameworkBuilder {

  def build(): RdfFramework = new RdfFrameworkInstance()
}
