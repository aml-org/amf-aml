package amf.aml.internal.parse.dialects.nodemapping.like

import amf.aml.client.scala.model.domain.ConditionalNodeMapping
import amf.aml.internal.metamodel.domain.ConditionalNodeMappingModel.{Else, If, Then}
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.parse.dialects.nodemapping.like.ConditionalNodeMappingParser.identifierKey
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.domain.{AmfScalar, DomainElement}
import amf.core.internal.metamodel.Field
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, ValueNode}
import org.yaml.model.{YMap, YScalar, YType}

class ConditionalNodeMappingParser(implicit ctx: DialectContext) extends NodeMappingLikeParserInterface {

  override def parse(map: YMap, adopt: DomainElement => Any, isFragment: Boolean): ConditionalNodeMapping = {

    val conditionalNodeMapping = ConditionalNodeMapping(map)

    adopt(conditionalNodeMapping)

    map.key(
        identifierKey,
        entry =>
          entry.value.tagType match {
            case YType.Map =>
              val innerMap = entry.value.as[YMap]
              parseConditionalField(innerMap, If, "if", conditionalNodeMapping)
              parseConditionalField(innerMap, Then, "then", conditionalNodeMapping)
              parseConditionalField(innerMap, Else, "else", conditionalNodeMapping)
            case _ =>
              ctx.eh.violation(DialectError,
                               conditionalNodeMapping.id,
                               s"Conditional mapping must be a map",
                               entry.value.location)
        }
    )

    if (!isFragment) ctx.closedNode("conditionalMapping", conditionalNodeMapping.id, map)

    conditionalNodeMapping
  }

  private def parseConditionalField(map: YMap,
                                    field: Field,
                                    key: String,
                                    conditionalNodeMapping: ConditionalNodeMapping): Unit = {
    map.key(
        key,
        entry => {
          val node = ValueNode(entry.value).string()
          conditionalNodeMapping.set(field, node, Annotations(entry))
        }
    )
  }
}

object ConditionalNodeMappingParser {

  val identifierKey: String = "conditional"

  def apply()(implicit ctx: DialectContext) = new ConditionalNodeMappingParser
}
