package amf.plugins.document.vocabularies.parser.dialects.property.like

import amf.core.parser.{SearchScope, ValueNode, YMapOps}
import amf.core.utils.AmfStrings
import amf.core.vocabulary.Namespace
import amf.plugins.document.vocabularies.metamodel.domain.PropertyLikeMappingModel
import amf.plugins.document.vocabularies.model.domain.PropertyLikeMapping
import amf.plugins.document.vocabularies.parser.dialects.DialectContext
import amf.validation.DialectValidations.DialectError
import org.yaml.model.YMap

case class PropertyTermParser(map: YMap, propertyLikeMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel])(
    implicit val ctx: DialectContext) {

  /**
    * Requires that the property like mapping has a name for fallback to work
    */
  def parse(): Unit = {
    map.key("propertyTerm") match {
      case Some(e) =>
        val value          = ValueNode(e.value)
        val propertyTermId = value.string().toString
        ctx.declarations.findPropertyTerm(propertyTermId, SearchScope.All) match {
          case Some(propertyTerm) =>
            propertyLikeMapping.withNodePropertyMapping(propertyTerm.id)
          case _ =>
            ctx.eh.violation(DialectError,
                             propertyLikeMapping.id,
                             s"Cannot find property term with alias $propertyTermId",
                             e.value)
        }
      case _ =>
        val name = propertyLikeMapping.name().value()
        propertyLikeMapping.withNodePropertyMapping((Namespace.Data + name.urlComponentEncoded).iri())
    }
  }
}
