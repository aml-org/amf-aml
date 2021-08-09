package amf.rdf.internal.parsers

import amf.core.client.scala.model.domain.ScalarNode
import amf.rdf.client.scala.Literal

object DynamicLiteralParser {
  def parse(l: Literal): ScalarNode = {
    val result = ScalarNode()
    l.literalType.foreach(t => result.withDataType(t))
    result.withValue(l.value)
    result
  }
}
