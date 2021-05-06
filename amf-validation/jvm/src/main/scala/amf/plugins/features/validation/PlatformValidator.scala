package amf.plugins.features.validation

object PlatformValidator {
  def instance = new SHACLValidator
}
