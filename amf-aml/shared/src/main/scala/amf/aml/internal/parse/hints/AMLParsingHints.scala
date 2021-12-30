package amf.aml.internal.parse.hints

import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.internal.parser.{Root, YNodeLikeOps}
import org.yaml.model.YMap
import amf.core.internal.parser.YMapOps

object YamlDirectiveComment {
  def from(root: Root): Option[String] = root.parsed.comment
}

object $DialectPropertyValue extends PropertyValue {
  override val key: String = "$dialect"
}

object $TypePropertyValue extends PropertyValue {
  override val key: String = "$type"
}

trait PropertyValue {
  val key: String
  def from(root: Root): Option[String] = {
    root.parsed match {
      case parsedInput: SyamlParsedDocument =>
        for {
          yMap            <- parsedInput.document.node.toOption[YMap]
          $typeEntry      <- yMap.key(key)
          $typeEntryValue <- $typeEntry.value.asScalar
        } yield {
          $typeEntryValue.text
        }
      case _ => None
    }
  }
}
