package amf.rdf.internal

import amf.core.client.scala.model.document.SourceMap
import amf.core.client.scala.model.domain.ResolvableAnnotation
import amf.core.internal.metamodel.Type
import amf.core.internal.metamodel.Type._
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.Annotations
import org.yaml.model.{YMap, YNode, YType}

trait RdfParserCommon {

  implicit val ctx: RdfParserContext

  def annots(sources: SourceMap, key: String): Annotations =
    ctx.annotationsFacade
      .retrieveAnnotation(ctx.nodes, sources, key)
      .into(ctx.collected, _.isInstanceOf[ResolvableAnnotation])

  @scala.annotation.tailrec
  final def value(t: Type, node: YNode): YNode = {
    node.tagType match {
      case YType.Seq =>
        t match {
          case Array(_) => node
          case _        => value(t, node.as[Seq[YNode]].head)
        }
      case YType.Map =>
        val m: YMap = node.as[YMap]
        t match {
          case Iri                                       => m.key("@id").get.value
          case Str | RegExp | Bool | Type.Int | Type.Any => m.key("@value").get.value
          case _                                         => node
        }
      case _ => node
    }
  }
}
