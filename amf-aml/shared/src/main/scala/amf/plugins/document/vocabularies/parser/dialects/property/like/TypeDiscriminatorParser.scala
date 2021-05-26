package amf.plugins.document.vocabularies.parser.dialects.property.like

import amf.core.parser.{Annotations, YMapOps}
import amf.plugins.document.vocabularies.metamodel.domain.PropertyLikeMappingModel
import amf.plugins.document.vocabularies.model.domain.PropertyLikeMapping
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
