package amf.validation.internal

object PlatformValidator {
  val instance = () => new SHACLValidator
}
