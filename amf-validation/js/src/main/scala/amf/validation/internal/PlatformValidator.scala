package amf.validation.internal

import amf.validation.client.platform.SHACLValidator

object PlatformValidator {
  val instance = () => new SHACLValidator
}
