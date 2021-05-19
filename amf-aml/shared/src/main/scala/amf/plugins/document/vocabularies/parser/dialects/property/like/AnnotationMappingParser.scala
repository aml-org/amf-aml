package amf.plugins.document.vocabularies.parser.dialects.property.like

import amf.core.parser.{Annotations, SearchScope, YMapOps}
import amf.plugins.document.vocabularies.metamodel.domain.AnnotationMappingModel
import amf.plugins.document.vocabularies.model.domain.AnnotationMapping
import amf.plugins.document.vocabularies.parser.common.AnnotationsParser
import amf.plugins.document.vocabularies.parser.dialects.DialectContext
import amf.validation.DialectValidations.DialectError
import org.yaml.model.{YMap, YMapEntry, YType}

case class AnnotationMappingParser(entry: YMapEntry, parent: String)(implicit val ctx: DialectContext)
    extends AnnotationsParser {
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
                     entry.value)
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
