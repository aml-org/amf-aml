package amf.aml.internal.render.emitters.dialects

import amf.core.internal.annotations.Aliases.{Alias, ImportLocation}
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.render.BaseEmitters.traverse
import amf.core.internal.render.emitters.EntryEmitter
import amf.core.client.scala.model.document.DeclaresModel
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.internal.render.SpecOrdering
import amf.aml.internal.render.emitters.common.ExternalEmitter
import amf.aml.internal.render.emitters.instances.AmlEmittersHelper
import amf.aml.client.scala.model.document.{Dialect, Vocabulary}
import amf.aml.client.scala.model.domain.{
  AnnotationMapping,
  ConditionalNodeMapping,
  NodeMappable,
  NodeMapping,
  UnionNodeMapping
}
import org.yaml.model.YDocument.EntryBuilder

trait DialectDocumentsEmitters extends AmlEmittersHelper {

  val dialect: Dialect
  val aliases: Map[RefKey, (Alias, ImportLocation)]

  override protected def referenceIndexKeyFor(unit: DeclaresModel): RefKey = unit match {
    case v: Vocabulary => v.base.value()
    case _             => unit.id
  }

  protected def buildExternalsAliasIndexFrom(dialect: Dialect): Map[RefKey, (Alias, ImportLocation)] = {
    dialect.externals.map { e =>
      e.base.value() -> (e.alias.value(), "")
    }.toMap
  }

  def rootLevelEmitters(ordering: SpecOrdering): Seq[EntryEmitter] =
    Seq(ReferencesEmitter(dialect, ordering, aliases)) ++
      nodeMappingDeclarationEmitters(dialect, ordering, aliases) ++
      annotationMappingDeclarationEmitters(dialect, aliases, ordering) ++
      extensionsEmitter(dialect, aliases, ordering) ++
      externalEmitters(ordering)

  def externalEmitters(ordering: SpecOrdering): Seq[EntryEmitter] = {
    if (dialect.externals.nonEmpty) {
      Seq(new EntryEmitter {
        override def emit(b: EntryBuilder): Unit = {
          b.entry("external", _.obj({ b =>
            traverse(ordering.sorted(dialect.externals.map(external => ExternalEmitter(external, ordering))), b)
          }))
        }

        override def position(): Position = {
          dialect.externals
            .flatMap(e => e.annotations.find(classOf[LexicalInformation]).map(_.range.start))
            .sortBy(_.line)
            .headOption
            .getOrElse(ZERO)
        }
      })
    } else {
      Nil
    }
  }

  private def nodeMappingDeclarationEmitters(dialect: Dialect,
                                             ordering: SpecOrdering,
                                             aliases: Map[String, (String, String)]): Seq[EntryEmitter] = {
    type NodeMappable = NodeMappable.AnyNodeMappable
    val nodeMappingDeclarations: Seq[NodeMappable] = dialect.declares.collect {
      case nm: NodeMapping            => nm
      case um: UnionNodeMapping       => um
      case cm: ConditionalNodeMapping => cm
    }
    if (nodeMappingDeclarations.nonEmpty)
      Seq(NodeMappingsEntryEmitter(dialect, nodeMappingDeclarations, aliases, ordering))
    else Nil
  }

  private def annotationMappingDeclarationEmitters(dialect: Dialect,
                                                   aliases: Map[String, (String, String)],
                                                   ordering: SpecOrdering): Seq[EntryEmitter] = {
    val annotationMappings = dialect.declares.collect {
      case mapping: AnnotationMapping => mapping
    }
    if (annotationMappings.nonEmpty)
      Seq(AnnotationMappingsEntryEmitter(dialect, annotationMappings, aliases, ordering))
    else Seq.empty
  }

  private def extensionsEmitter(dialect: Dialect,
                                aliases: Map[String, (String, String)],
                                ordering: SpecOrdering): Seq[EntryEmitter] = {
    if (dialect.extensions().nonEmpty) Seq(ExtensionMappingsEntryEmitter(dialect, aliases, ordering))
    else Seq.empty
  }
}
