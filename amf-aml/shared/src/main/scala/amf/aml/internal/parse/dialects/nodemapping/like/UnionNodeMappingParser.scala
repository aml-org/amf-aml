package amf.aml.internal.parse.dialects.nodemapping.like

import amf.aml.client.scala.model.domain.UnionNodeMapping
import amf.aml.internal.metamodel.domain.UnionNodeMappingModel
import amf.aml.internal.metamodel.domain.UnionNodeMappingModel.ObjectRange
import amf.aml.internal.parse.dialects.DialectAstOps.{DialectScalarValueEntryParserOpts, DialectYMapOps}
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar, DomainElement}
import amf.core.internal.parser.domain.Annotations
import org.yaml.model.{YMap, YScalar, YSequence, YType}

class UnionNodeMappingParser(implicit ctx: DialectContext)
    extends NodeMappingLikeParserInterface
    with AnyMappingParser {

  override def parse(map: YMap, adopt: DomainElement => Any, isFragment: Boolean): UnionNodeMapping = {

    val unionNodeMapping = UnionNodeMapping(map)
    adopt(unionNodeMapping)

    super.parse(map, unionNodeMapping)

    map.key(
      "union",
      entry => {
        entry.value.tagType match {
          case YType.Seq =>
            try {
              val nodes = MappingParsingHelper.entrySeqNodesToString(entry)
              unionNodeMapping.set(ObjectRange, AmfArray(nodes, Annotations(entry.value)), Annotations(entry))
            } catch {
              case _: Exception =>
                ctx.eh.violation(
                  DialectError,
                  unionNodeMapping.id,
                  s"Union node mappings must be declared as lists of node mapping references",
                  entry.value.location
                )
            }
          case _ =>
            ctx.eh.violation(
              DialectError,
              unionNodeMapping.id,
              s"Union node mappings must be declared as lists of node mapping references",
              entry.value.location
            )
        }
      }
    )

    map.key(
      "typeDiscriminator",
      entry => {
        val types = entry.value.as[YMap]
        val typeMapping = types.entries.foldLeft(Map[String, String]()) { case (acc, e) =>
          val nodeMappingId = e.value.as[YScalar].text
          acc + (e.key.as[YScalar].text -> nodeMappingId)
        }
        unionNodeMapping.withTypeDiscriminator(typeMapping, Annotations(entry), Annotations(types))
      }
    )

    map.parse("typeDiscriminatorName", unionNodeMapping setParsing UnionNodeMappingModel.TypeDiscriminatorName)

    unionNodeMapping
  }

}

object UnionNodeMappingParser {

  val identifierKey: String = "union"

  def apply()(implicit ctx: DialectContext) = new UnionNodeMappingParser
}
