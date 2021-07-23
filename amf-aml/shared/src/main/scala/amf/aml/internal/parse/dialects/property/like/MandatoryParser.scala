package amf.aml.internal.parse.dialects.property.like

import amf.core.client.scala.model.domain.AmfScalar
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, ScalarNode}
import amf.aml.internal.metamodel.domain.PropertyLikeMappingModel
import amf.aml.client.scala.model.domain.PropertyLikeMapping
import amf.aml.internal.parse.dialects.DialectContext
import org.yaml.model.YMap

case class MandatoryParser(map: YMap, propertyLikeMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel])(
    implicit val ctx: DialectContext) {
  def parse(): Unit = {
    map.key(
        "mandatory",
        entry => {
          val required = ScalarNode(entry.value).boolean().toBool
          val value    = if (required) 1 else 0
          propertyLikeMapping.set(propertyLikeMapping.meta.MinCount,
                                  AmfScalar(value, Annotations(entry.value)),
                                  Annotations(entry))
        }
    )
  }

}