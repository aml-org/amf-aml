package amf.aml.internal.parse.dialects.property.like

import amf.core.client.scala.vocabulary.Namespace
import amf.aml.internal.metamodel.domain.PropertyLikeMappingModel
import amf.aml.client.scala.model.domain.{PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.parse.dialects.DialectAstOps.{DialectYMapOps, _}
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.validate.DialectValidations
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.internal.parser.domain.SearchScope.All
import amf.core.internal.parser.domain.{Annotations, ValueNode}
import org.yaml.model.{YMap, YPart}

case class PropertyLikeMappingParser[T <: PropertyLikeMapping[_ <: PropertyLikeMappingModel]](
    map: YMap,
    propertyLikeMapping: T
)(implicit val ctx: DialectContext) {
  def parse(): Unit = {
    val meta = propertyLikeMapping.meta

    map.parse("pattern", propertyLikeMapping setParsing meta.Pattern)
    map.parse("minimum", propertyLikeMapping setParsing meta.Minimum)
    map.parse("unique", propertyLikeMapping setParsing meta.Unique)
    map.parse("maximum", propertyLikeMapping setParsing meta.Maximum)
    map.parse("allowMultiple", propertyLikeMapping setParsing meta.AllowMultiple)
    map.parse("sorted", propertyLikeMapping setParsing meta.Sorted)
    map.parse("typeDiscriminatorName", propertyLikeMapping setParsing meta.TypeDiscriminatorName)

    parseMapKey()
    parseMapValue()

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

  private def parseMapKey(): Unit = {
    val mapKey = map.key("mapKey")
    val mapTermKey = map.key("mapTermKey")

    for {
      _ <- mapKey
      _ <- mapTermKey
    } yield {
      ctx.eh.violation(DialectError, propertyLikeMapping.id, s"mapKey and mapTermKey are mutually exclusive", map.location)
    }

    mapTermKey.fold({
      mapKey.foreach(entry => {
        val propertyLabel = ValueNode(entry.value).string().toString
        propertyLikeMapping.withMapKeyProperty(propertyLabel, Annotations(entry.value))
      })
    })(entry => {
      val propertyTermId = ValueNode(entry.value).string().toString
      getTermIfValid(propertyTermId, propertyLikeMapping.id, entry.value).foreach { p =>
        propertyLikeMapping.withMapTermKeyProperty(p, Annotations(entry.value))
      }
    })
  }

  private def parseMapValue(): Unit = {
    val mapValue = map.key("mapValue")
    val mapTermValue = map.key("mapTermValue")

    for {
      _ <- mapValue
      _ <- mapTermValue
    } yield {
      ctx.eh
        .violation(DialectError, propertyLikeMapping.id, s"mapValue and mapTermValue are mutually exclusive", map.location)
    }

    mapTermValue.fold({
      mapValue.foreach(entry => {
        val propertyLabel = ValueNode(entry.value).string().toString
        propertyLikeMapping.withMapValueProperty(propertyLabel, Annotations(entry.value))
      })
    })(entry => {
      val propertyTermId = ValueNode(entry.value).string().toString
      getTermIfValid(propertyTermId, propertyLikeMapping.id, entry.value).foreach { p =>
        propertyLikeMapping.withMapTermValueProperty(p, Annotations(entry.value))
      }
    })

  }

  private def getTermIfValid(iri: String, propertyMappingId: String, ast: YPart): Option[String] = {
    Namespace(iri).base match {
      case Namespace.Data.base => Some(iri)
      case _ =>
        ctx.declarations.findPropertyTerm(iri, All) match {
          case Some(term) => Some(term.id)
          case _ =>
            ctx.eh
              .violation(DialectError, propertyMappingId, s"Cannot find property term with alias $iri", ast.location)
            None
        }
    }
  }

}
