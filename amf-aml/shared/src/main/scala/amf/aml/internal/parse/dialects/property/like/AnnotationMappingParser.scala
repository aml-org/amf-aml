package amf.aml.internal.parse.dialects.property.like

import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import amf.aml.internal.metamodel.domain.AnnotationMappingModel
import amf.aml.client.scala.model.domain.AnnotationMapping
import amf.aml.internal.parse.common.AnnotationsParser
import amf.aml.internal.parse.common.AnnotationsParser.parseAnnotations
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.validate.DialectValidations.DialectError
import org.yaml.model.{YMap, YMapEntry, YType}

case class AnnotationMappingParser(entry: YMapEntry, parent: String)(implicit val ctx: DialectContext) {
  def parse(): Option[AnnotationMapping] = {
    val name = entry.key.toString
    entry.value.tagType match {
      case YType.Map =>
        val map = entry.value.as[YMap]
        val annotationMapping = AnnotationMapping(map)
          .set(AnnotationMappingModel.Name, name, Annotations(entry.key))
          .withId(s"$parent/$name")
        ctx.closedNode("annotationMapping", annotationMapping.id, map)

        PropertyLikeMappingParser(map, annotationMapping).parse()

        parseDomain(map, annotationMapping)
        parseAnnotations(map, annotationMapping, ctx.declarations)
        Some(annotationMapping)
      case t =>
        ctx.eh
          .violation(DialectError,
                     parent,
                     s"Invalid type $t (expected ${YType.Map}) for annotation mapping node $name",
                     entry.value.location)
        None
    }
  }

  private def parseDomain(ast: YMap, parsedAnnotationMapping: AnnotationMapping): Unit = {
    for {
      domainEntry <- ast.key("domain")
      classTerm   <- ctx.declarations.findClassTerm(domainEntry.value.toString, SearchScope.All)
    } yield {
      parsedAnnotationMapping.set(AnnotationMappingModel.Domain, classTerm.id, Annotations(domainEntry))
    }
  }

}
