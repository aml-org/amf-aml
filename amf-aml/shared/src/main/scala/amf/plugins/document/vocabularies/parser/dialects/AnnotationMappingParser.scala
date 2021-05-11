package amf.plugins.document.vocabularies.parser.dialects

import amf.plugins.document.vocabularies.model.domain.AnnotationMapping
import org.yaml.model.{YMap, YMapEntry, YType}
import amf.core.parser.{Annotations, SearchScope, YMapOps}
import amf.plugins.document.vocabularies.metamodel.domain.AnnotationMappingModel
import amf.validation.DialectValidations.DialectError

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
        PropertyTermParser(map, annotationMapping).parse()
        RangeParser(map, annotationMapping).parse()
        parseTarget(map, annotationMapping)
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

  private def parseTarget(ast: YMap, parsedAnnotationMapping: AnnotationMapping): Unit = {
    for {
      targetEntry <- ast.key("target")
      classTerm   <- ctx.declarations.findClassTerm(targetEntry.value.toString, SearchScope.All)
    } yield {
      parsedAnnotationMapping.set(AnnotationMappingModel.Target, classTerm.id, Annotations(targetEntry))
    }
  }

}
