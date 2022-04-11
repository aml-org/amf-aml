package amf.aml.internal.parse.dialects.nodemapping.like

import amf.aml.client.scala.model.domain.AnyMapping
import amf.aml.internal.metamodel.domain.AnyMappingModel
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.internal.metamodel.Field
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.Annotations
import org.yaml.model._

trait AnyMappingParser {

  def parse(map: YMap, mapping: AnyMapping)(implicit ctx: DialectContext): AnyMapping = {

    processAnyMappingField(map, mapping, "allOf", AnyMappingModel.And)
    processAnyMappingField(map, mapping, "oneOf", AnyMappingModel.Or)
    processAnyMappingField(map, mapping, "components", AnyMappingModel.Components)
    ConditionalParser.parse(map, mapping)
    mapping

  }

  private def processAnyMappingField(map: YMap, mapping: AnyMapping, key: String, field: Field)(
      implicit ctx: DialectContext): Unit = map.key(
      key,
      entry => {
        entry.value.tagType match {
          case YType.Seq =>
            val nodes = MappingParsingHelper.entrySeqNodesToString(entry)
            mapping.set(field, AmfArray(nodes, Annotations(entry.value)), Annotations(entry))
          case _ =>
            ctx.eh.violation(DialectError,
                             mapping.id,
                             s"$key mappings must be declared as lists of node mapping references",
                             entry.value.location)
        }
      }
  )
}
