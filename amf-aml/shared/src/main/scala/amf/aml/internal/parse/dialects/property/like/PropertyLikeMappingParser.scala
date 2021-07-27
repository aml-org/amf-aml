package amf.aml.internal.parse.dialects.property.like

import amf.core.client.scala.vocabulary.Namespace
import amf.aml.internal.metamodel.domain.PropertyLikeMappingModel
import amf.aml.client.scala.model.domain.{PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.parse.dialects.DialectAstOps.{DialectYMapOps, _}
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.validate.DialectValidations
import org.yaml.model.{YMap, YPart}

case class PropertyLikeMappingParser[T <: PropertyLikeMapping[_ <: PropertyLikeMappingModel]](
    map: YMap,
    propertyLikeMapping: T)(implicit val ctx: DialectContext) {
  def parse(): Unit = {
    val meta = propertyLikeMapping.meta

    map.parse("pattern", propertyLikeMapping setParsing meta.Pattern)
    map.parse("minimum", propertyLikeMapping setParsing meta.Minimum)
    map.parse("unique", propertyLikeMapping setParsing meta.Unique)
    map.parse("maximum", propertyLikeMapping setParsing meta.Maximum)
    map.parse("allowMultiple", propertyLikeMapping setParsing meta.AllowMultiple)
    map.parse("sorted", propertyLikeMapping setParsing meta.Sorted)
    map.parse("typeDiscriminatorName", propertyLikeMapping setParsing meta.TypeDiscriminatorName)

    MandatoryParser(map, propertyLikeMapping).parse()
    PropertyTermParser(map, propertyLikeMapping).parse()
    RangeParser(map, propertyLikeMapping).parse()
    EnumParser(map, propertyLikeMapping).parse()
    TypeDiscriminatorParser(map, propertyLikeMapping).parse()
    ExternalLinksParser(map, propertyLikeMapping).parse() // TODO: check dependencies among properties

    validateUniqueGUIDConstraint(propertyLikeMapping, map)
  }

  // We check that if this is a GUID it also has the unique constraint
  private def validateUniqueGUIDConstraint(propertyLikeMapping: T, ast: YPart): Unit = {
    propertyLikeMapping.literalRange().option() foreach { literal =>
      if (literal == (Namespace.Shapes + "guid").iri() && !propertyLikeMapping.unique().option().getOrElse(false)) {
        ctx.eh.warning(
            DialectValidations.GuidRangeWithoutUnique,
            propertyLikeMapping.id,
            s"Declaration of property '${propertyLikeMapping.name().value()}' with range GUID and without unique constraint",
            ast.location
        )
      }
    }
  }

}
