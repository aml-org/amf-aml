package amf.aml.internal.parse.dialects.property.like

import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.Annotations
import amf.aml.internal.metamodel.domain.PropertyLikeMappingModel
import amf.aml.client.scala.model.domain.PropertyLikeMapping
import org.yaml.model.{YMap, YScalar}

case class TypeDiscriminatorParser(map: YMap, propertyLikeMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel]) {
  def parse(): Unit = {
    map.key(
        "typeDiscriminator",
        entry => {
          val types = entry.value.as[YMap]
          val typeMapping = types.entries.foldLeft(Map[String, String]()) {
            case (acc, e) =>
              val nodeMappingId = e.value.as[YScalar].text
              acc + (e.key.as[YScalar].text -> nodeMappingId)
          }
          propertyLikeMapping.withTypeDiscriminator(typeMapping, Annotations(entry), Annotations(types))
        }
    )
  }

}
