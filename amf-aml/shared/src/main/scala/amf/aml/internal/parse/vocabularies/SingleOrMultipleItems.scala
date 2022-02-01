package amf.aml.internal.parse.vocabularies

import amf.core.internal.parser.domain.{DefaultArrayNode, ValueNode}
import org.yaml.model.{YMapEntry, YType}

trait SingleOrMultipleItems {

  protected def singleOrMultipleItemsAsString(entry: YMapEntry) = {
    entry.value.tagType match {
      case YType.Str  => Seq(ValueNode(entry.value).string().toString)
      case YType.Seq  => DefaultArrayNode(node = entry.value).nodes._1.map(_.value.toString)
      case YType.Null => Seq.empty
    }
  }
}
