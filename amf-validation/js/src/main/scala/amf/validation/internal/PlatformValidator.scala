package amf.validation.internal

import amf.core.client.scala.config.AMFEventListener
import amf.validation.client.platform.SHACLValidator

object PlatformValidator {
  val instance: Seq[AMFEventListener] => SHACLValidator = listeners => new SHACLValidator(listeners)
}
