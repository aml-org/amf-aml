package amf.aml.internal.parse.vocabularies

import amf.core.internal.parser.domain.{DefaultArrayNode, ValueNode}
import org.yaml.model.{YMapEntry, YSequence, YType}

trait SingleOrMultipleItems {

  protected def singleOrMultipleItemsAsString(entry: YMapEntry) = {
    entry.value.tagType match {
      case YType.Str  => Seq(ValueNode(entry.value).text().toString)
      case YType.Seq  => entry.value.as[YSequence].nodes.map(_.as[String])
      case YType.Null => Seq.empty
    }
  }
}
