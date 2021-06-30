package amf.validation.internal

import amf.core.client.scala.config.AMFEventListener

object PlatformValidator {
  val instance: Seq[AMFEventListener] => SHACLValidator = listeners => new SHACLValidator(listeners)
}
