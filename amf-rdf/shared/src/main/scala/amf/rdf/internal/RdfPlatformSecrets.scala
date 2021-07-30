package amf.rdf.internal

import amf.rdf.client.platform.RdfFrameworkBuilder

trait RdfPlatformSecrets {

  val framework: RdfFramework = RdfFrameworkBuilder.build()
}
