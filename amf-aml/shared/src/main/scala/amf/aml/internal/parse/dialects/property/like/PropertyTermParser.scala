package amf.aml.internal.parse.dialects.property.like

import amf.core.client.scala.model.domain.AmfScalar
import amf.core.internal.parser.YMapOps
import amf.core.internal.utils.AmfStrings
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.domain.{Annotations, SearchScope, ValueNode}
import amf.aml.internal.metamodel.domain.PropertyLikeMappingModel
import amf.aml.internal.metamodel.domain.PropertyMappingModel.NodePropertyMapping
import amf.aml.client.scala.model.domain.PropertyLikeMapping
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.validate.DialectValidations.DialectError
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
