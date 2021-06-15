package amf.aml.internal.parse.instances

import amf.core.internal.parser.YMapOps
import org.yaml.model.{YMap, YMapEntry}
import amf.aml.internal.parse.instances.BaseDirective.baseFrom

import scala.util.matching.Regex

/**
  * Base of an URI (spec definition): the beginning part of a URI until the first (inclusive) '#' character or else
  * the first '/' character (excluding the ones from the protocol) if no '#' character is defined
  */
trait BaseDirectiveOverride {

  def overrideBase(uri: String, baseEntry: YMapEntry): String = {
    val replacement = baseEntry.value.toString
    val base        = baseFrom(uri)
    uri.replace(base, replacement)
  }

  def overrideBase(id: String, map: YMap): String = {
    map.key("$base") match {
      case Some(baseEntry) => overrideBase(id, baseEntry)
      case _               => id
    }
  }
}

object BaseDirective {
  val HashRegex: Regex  = "(http://|file://)?([^#]*)#(.*)".r("protocol", "base", "tail")
  val SlashRegex: Regex = "(http://|file://)?([^/]*)/(.*)".r("protocol", "base", "tail")

  /**
    * Extracts the base from the supplied URI
    * @param uri input uri
    * @return base if defined, whole uri otherwise
    */
  def baseFrom(uri: String): String = {
    uri match {
      case BaseDirective.HashRegex(protocol, base, _)  => protocol + base + "#"
      case BaseDirective.SlashRegex(protocol, base, _) => protocol + base + "/"
      case _                                           => uri
    }
  }
}
