package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.LexicalInformation
import amf.core.emitter.BaseEmitters.{MapEntryEmitter, pos, traverse}
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.metamodel.Field
import amf.core.model.StrField
import amf.core.model.domain.DomainElement
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.metamodel.domain.AnnotationMappingModel
import amf.plugins.document.vocabularies.metamodel.domain.AnnotationMappingModel.Domain
import amf.plugins.document.vocabularies.metamodel.domain.PropertyMappingModel.NodePropertyMapping
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.AnnotationMapping
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YType

import scala.collection.mutable.ArrayBuffer

case class AnnotationMappingsEntryEmitter(dialect: Dialect,
                                          annotationMappings: Seq[AnnotationMapping],
                                          aliases: Map[String, (String, String)],
                                          ordering: SpecOrdering)
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
                                    ordering: SpecOrdering)
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
