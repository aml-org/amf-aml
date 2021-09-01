package amf.rdf.internal.unsafe

import amf.rdf.client.scala.RdfFramework
import amf.rdf.internal.RdfFrameworkBuilder

trait RdfPlatformSecrets {

  protected val framework: RdfFramework = RdfFrameworkBuilder.build()
}
