package amf.aml.internal.parse.dialects.nodemapping.like

import amf.aml.client.scala.model.domain.ConditionalNodeMapping
import amf.aml.internal.metamodel.domain.ConditionalNodeMappingModel.{Else, If, Then}
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.domain.{AmfScalar, DomainElement}
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.Annotations
import org.yaml.model.{YMap, YScalar, YType}

class ConditionalNodeMappingParser(implicit ctx: DialectContext) extends NodeMappingLikeParserInterface {

  override def parse(map: YMap, adopt: DomainElement => Any, isFragment: Boolean): ConditionalNodeMapping = {

    val conditionalNodeMapping = ConditionalNodeMapping(map)

    adopt(conditionalNodeMapping)

    //TODO implement close shape for conditionalNodeMapping
    //    if(!fragment)
    //    ctx.closedNode("conditionalNodeMapping", conditionalNodeMapping.id, map)

    map.key(
        "if",
        entry => {
          entry.value.tagType match {
            case YType.Str =>
              val value = entry.value.as[YScalar].text
              conditionalNodeMapping.set(If, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
            case _ =>
              ctx.eh.violation(DialectError,
                               conditionalNodeMapping.id,
                               s"If field in a conditional node mappings must be a declared node mapping reference",
                               entry.value.location)
          }
        }
    )

    map.key(
        "then",
        entry => {
          entry.value.tagType match {
            case YType.Str =>
              val value = entry.value.as[YScalar].text
              conditionalNodeMapping.set(Then, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
            case _ =>
              ctx.eh.violation(DialectError,
                               conditionalNodeMapping.id,
                               s"Then field in a conditional node mappings must be a declared node mapping reference",
                               entry.value.location)
          }
        }
    )

    map.key(
        "else",
        entry => {
          entry.value.tagType match {
            case YType.Str =>
              val value = entry.value.as[YScalar].text
              conditionalNodeMapping.set(Else, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
            case _ =>
              ctx.eh.violation(DialectError,
                               conditionalNodeMapping.id,
                               s"Else field in a conditional node mappings must be a declared node mapping reference",
                               entry.value.location)
          }
        }
    )

    conditionalNodeMapping
  }

}

object ConditionalNodeMappingParser {

  val identifierKey: String = "if"

  def apply()(implicit ctx: DialectContext) = new ConditionalNodeMappingParser
}
