package amf.aml.internal.unsafe

import amf.core.internal.unsafe.PlatformSecrets
import amf.aml.client.platform.RdfFrameworkInstance
private[amf] object RdfFrameworkSecret extends PlatformSecrets {
  def init() = {
    platform.rdfFramework = Some(new RdfFrameworkInstance())
  }
}
