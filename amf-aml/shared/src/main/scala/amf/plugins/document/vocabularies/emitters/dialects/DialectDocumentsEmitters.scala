package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.Aliases.{Alias, ImportLocation}
import amf.core.annotations.LexicalInformation
import amf.core.emitter.BaseEmitters.traverse
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.model.document.DeclaresModel
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.emitters.common.ExternalEmitter
import amf.plugins.document.vocabularies.emitters.instances.AmlEmittersHelper
import amf.plugins.document.vocabularies.model.document.{Dialect, Vocabulary}
import amf.plugins.document.vocabularies.model.domain.{AnnotationMapping, NodeMappable, NodeMapping, UnionNodeMapping}
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
      case nm: NodeMapping      => nm
      case um: UnionNodeMapping => um
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
