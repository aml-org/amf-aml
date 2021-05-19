package amf.plugins.document.vocabularies.parser.dialects.property.like

import amf.core.parser.YMapOps
import amf.plugins.document.vocabularies.metamodel.domain.PropertyLikeMappingModel
import amf.plugins.document.vocabularies.model.domain.PropertyLikeMapping
import amf.plugins.document.vocabularies.parser.dialects.DialectContext
import amf.validation.DialectValidations.DialectError
import org.yaml.model.YMap

case class ExternalLinksParser(map: YMap, propertyLikeMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel])(
    implicit ctx: DialectContext) {
  def parse(): Unit = {
    map.key(
        "isLink",
        entry => {
          val isLink = entry.value.as[Boolean]
          propertyLikeMapping.withExternallyLinkable(isLink)
          propertyLikeMapping.literalRange().option() match {
            case Some(v) =>
              ctx.eh.violation(
                  DialectError,
                  s"Aml links support in property mappings only can be declared in object properties but scalar range detected: $v",
                  entry.value)
            case _ => // ignore
          }
        }
    )
  }
}
