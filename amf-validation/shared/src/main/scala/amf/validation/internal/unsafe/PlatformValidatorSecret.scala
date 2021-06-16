package amf.validation.internal.unsafe

import amf.core.internal.unsafe.PlatformSecrets
import amf.validation.internal.PlatformValidator

private[amf] object PlatformValidatorSecret extends PlatformSecrets {
  def init() = {
    platform.rdfFramework = Some(PlatformValidator.instance())
  }
}
