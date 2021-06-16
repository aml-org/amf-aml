package amf.aml.internal.render.emitters.dialects

import amf.core.client.common.position.Position
import amf.core.internal.render.BaseEmitters.{pos, traverse}
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.aml.internal.render.emitters.instances.NodeMappableFinder
import amf.aml.internal.metamodel.domain.AnnotationMappingModel.Domain
import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.AnnotationMapping
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YType

import scala.collection.mutable.ArrayBuffer

case class AnnotationMappingsEntryEmitter(dialect: Dialect,
                                          annotationMappings: Seq[AnnotationMapping],
                                          aliases: Map[String, (String, String)],
                                          ordering: SpecOrdering)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AliasesConsumer
    with PosExtractor
    with GroupPosition {

  override def emit(b: EntryBuilder): Unit = {
    b.entry(
        "annotationMappings",
        _.obj { b =>
          val emitters =
            annotationMappings.map(mapping => AnnotationMappingEmitter(dialect, mapping, aliases, ordering))
          traverse(ordering.sorted(emitters), b)
        }
    )
  }

  override def position(): Position = groupPosition(annotationMappings)
}

case class AnnotationMappingEmitter(dialect: Dialect,
                                    element: AnnotationMapping,
                                    aliases: Map[String, (String, String)],
                                    ordering: SpecOrdering)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AliasEmitter {
  override def emit(b: EntryBuilder): Unit = {
    b.entry(
        element.name.value(),
        _.obj { b =>
          val emitters = ArrayBuffer.empty[EntryEmitter]
          emitters.appendAll(emitAlias("domain", element.domain(), Domain, YType.Str))
          emitters.appendAll(PropertyLikeMappingEmitter(dialect, element, ordering, aliases).emitters)
          traverse(ordering.sorted(emitters), b)
        }
    )
  }

  override def position(): Position = pos(element.annotations)
}