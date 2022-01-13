package amf.aml.internal.parse.dialects.property.like

import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import amf.aml.internal.metamodel.domain.AnnotationMappingModel
import amf.aml.client.scala.model.domain.AnnotationMapping
import amf.aml.internal.parse.common.AnnotationsParser
import amf.aml.internal.parse.common.AnnotationsParser.parseAnnotations
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.internal.annotations.SingleValueArray
import amf.core.internal.validation.CoreValidations.SyamlError
import amf.core.internal.validation.core.ValidationSpecification
import org.yaml.model.{YMap, YMapEntry, YPart, YSequence, YType}

import javax.naming.directory.SearchControls

case class AnnotationMappingParser(entry: YMapEntry, parent: String)(implicit val ctx: DialectContext) {
  def parse(): AnnotationMapping = {
    val name = entry.key.toString
    val annotationMapping = AnnotationMapping(Annotations(entry))
      .set(AnnotationMappingModel.Name, name, Annotations(entry.key))
      .withId(s"$parent/$name")
    entry.value.tagType match {
      case YType.Map =>
        val map = entry.value.as[YMap]
        ctx.closedNode("annotationMapping", annotationMapping.id, map)

        PropertyLikeMappingParser(map, annotationMapping).parse()

        parseDomain(map, annotationMapping)
        parseAnnotations(map, annotationMapping, ctx.declarations)
        annotationMapping
      case t =>
        ctx.eh
          .violation(DialectError,
                     parent,
                     s"Invalid type $t (expected ${YType.Map}) for annotation mapping node $name",
                     entry.value.location)
        annotationMapping
    }
  }

  private def parseDomain(ast: YMap, parsedAnnotationMapping: AnnotationMapping): Unit = {
    ast.key("domain").map { entry =>
      entry.value.tagType match {
        case YType.Str =>
          val classTerm = entry.value.asScalar.flatMap(scalar => scalarForClassTerm(scalar.text, scalar))
          parsedAnnotationMapping.set(AnnotationMappingModel.Domain,
                                      AmfArray(classTerm.toSeq, Annotations.virtual() += SingleValueArray()),
                                      Annotations(entry))
        case YType.Seq =>
          val nodes            = entry.value.as[YSequence]
          val classTermScalars = nodes.nodes.flatMap(node => scalarForClassTerm(node.value.toString, node))
          parsedAnnotationMapping
            .set(AnnotationMappingModel.Domain, AmfArray(classTermScalars, Annotations(nodes)), Annotations(entry))
        case other =>
          ctx.eh.violation(SyamlError, parsedAnnotationMapping, s"Expected array or string, got: $other", entry.value)
          parsedAnnotationMapping
      }
    }
  }

  private def scalarForClassTerm(value: String, ast: YPart): Option[AmfScalar] = {
    ctx.declarations.findClassTerm(value, SearchScope.All).map(x => AmfScalar(x.id, Annotations(ast)))
  }

}
