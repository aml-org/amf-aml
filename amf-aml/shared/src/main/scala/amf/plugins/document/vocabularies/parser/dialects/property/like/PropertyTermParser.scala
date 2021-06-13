package amf.plugins.document.vocabularies.parser.dialects.property.like

import amf.core.client.scala.model.domain.AmfScalar
import amf.core.internal.parser.YMapOps
import amf.core.internal.utils.AmfStrings
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.domain.{Annotations, SearchScope, ValueNode}
import amf.plugins.document.vocabularies.metamodel.domain.PropertyLikeMappingModel
import amf.plugins.document.vocabularies.metamodel.domain.PropertyMappingModel.NodePropertyMapping
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
            propertyLikeMapping.set(NodePropertyMapping,
                                    AmfScalar(propertyTerm.id, Annotations(e.value)),
                                    Annotations(e))
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
