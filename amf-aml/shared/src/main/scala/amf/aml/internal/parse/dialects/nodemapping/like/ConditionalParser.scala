package amf.aml.internal.parse.dialects.nodemapping.like

import amf.aml.client.scala.model.domain.AnyMapping
import amf.aml.internal.metamodel.domain.AnyMappingModel.{Else, If, Then}
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.internal.metamodel.Field
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, ValueNode}
import org.yaml.model.{YMap, YType}

object ConditionalParser {

  def parse(map: YMap, mapping: AnyMapping)(implicit ctx: DialectContext): Unit = {

    map.key(
      "conditional",
      entry =>
        entry.value.tagType match {
          case YType.Map =>
            val innerMap = entry.value.as[YMap]
            parseConditionalField(innerMap, If, "if", mapping)
            parseConditionalField(innerMap, Then, "then", mapping)
            parseConditionalField(innerMap, Else, "else", mapping)
            ctx.closedNode("conditionalMappingInner", mapping.id, innerMap)
          case _ =>
            ctx.eh.violation(DialectError, mapping.id, s"Conditional mapping must be a map", entry.value.location)
        }
    )
  }

  private def parseConditionalField(map: YMap, field: Field, key: String, mapping: AnyMapping): Unit = {
    map.key(
      key,
      entry => {
        val node = ValueNode(entry.value).string()
        mapping.set(field, node, Annotations(entry))
      }
    )
  }
}
