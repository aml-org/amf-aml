package amf.aml.internal.parse.dialects.property.like

import amf.core.client.scala.model.domain.AmfArray
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, ScalarNode}
import amf.aml.internal.metamodel.domain.PropertyLikeMappingModel
import amf.aml.client.scala.model.domain.PropertyLikeMapping
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.validate.DialectValidations.DialectError
import org.yaml.model.{YMap, YScalar, YSequence, YType}

case class EnumParser(map: YMap, propertyLikeMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel])(
    implicit ctx: DialectContext) {
  def parse(): Unit = {
    map.key(
        "enum",
        entry => {
          val seq = entry.value.as[YSequence]
          val values = seq.nodes.flatMap { node =>
            node.tagType match {
              case YType.Int   => Some(ScalarNode(node).integer())
              case YType.Float => Some(ScalarNode(node).double())
              case YType.Str   => Some(ScalarNode(node).string())
              case YType.Bool  => Some(ScalarNode(node).boolean())
              case _ =>
                ctx.eh.violation(DialectError, "Cannot create enumeration constraint from not scalar value", node)
                None
            }
          }
          propertyLikeMapping.set(propertyLikeMapping.meta.Enum,
                                  AmfArray(values, Annotations(seq)),
                                  Annotations(entry))
        }
    )
  }
}
