package amf.plugins.features.validation.unsafe

import amf.core.internal.unsafe.PlatformSecrets
import amf.plugins.features.validation.PlatformValidator

private[amf] object PlatformValidatorSecret extends PlatformSecrets {
  def init() = {
    platform.rdfFramework = Some(PlatformValidator.instance())
  }
}
