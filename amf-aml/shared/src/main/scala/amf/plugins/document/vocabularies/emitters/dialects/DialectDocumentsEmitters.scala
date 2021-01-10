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
import amf.plugins.document.vocabularies.model.domain.{AnnotationMapping, NodeMappable}
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
      annotationMappingDeclarationEmitters(dialect, ordering, aliases) ++
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
    }
    else {
      Nil
    }
  }

  def nodeMappingDeclarationEmitters(dialect: Dialect,
                                     ordering: SpecOrdering,
                                     aliases: Map[String, (String, String)]): Seq[EntryEmitter] = {
    val nodeMappingDeclarations: Seq[NodeMappable] = dialect.declares.collect {
      case nm: NodeMappable if !nm.isInstanceOf[AnnotationMapping] => nm
    }
    if (nodeMappingDeclarations.nonEmpty) {
      Seq(new EntryEmitter {
        override def emit(b: EntryBuilder): Unit = {
          b.entry(
              "nodeMappings",
              _.obj { b =>
                val nodeMappingEmitters: Seq[EntryEmitter] = nodeMappingDeclarations.map { n: NodeMappable =>
                  NodeMappingEmitter(dialect, n, ordering, aliases)
                }
                traverse(ordering.sorted(nodeMappingEmitters), b)
              }
          )
        }

        override def position(): Position = {
          nodeMappingDeclarations
            .map(
                _.annotations
                  .find(classOf[LexicalInformation])
                  .map(_.range.start)
                  .getOrElse(ZERO))
            .filter(_ != ZERO)
            .sorted
            .headOption
            .getOrElse(ZERO)
        }
      })
    }
    else {
      Nil
    }
  }

  def annotationMappingDeclarationEmitters(dialect: Dialect,
                                     ordering: SpecOrdering,
                                     aliases: Map[String, (String, String)]): Seq[EntryEmitter] = {
    val annotationMappingDeclarations: Seq[AnnotationMapping] = dialect.declares.collect {
      case nm: AnnotationMapping => nm
    }
    if (annotationMappingDeclarations.nonEmpty) {
      Seq(new EntryEmitter {
        override def emit(b: EntryBuilder): Unit = {
          b.entry(
            "annotationMappings",
            _.obj { b =>
              val nodeMappingEmitters: Seq[EntryEmitter] = annotationMappingDeclarations.map { am: AnnotationMapping =>
                PropertyMappingEmitter(dialect, am, ordering, aliases)
              }
              traverse(ordering.sorted(nodeMappingEmitters), b)
            }
          )
        }

        override def position(): Position = {
          annotationMappingDeclarations
            .map(
              _.annotations
                .find(classOf[LexicalInformation])
                .map(_.range.start)
                .getOrElse(ZERO))
            .filter(_ != ZERO)
            .sorted
            .headOption
            .getOrElse(ZERO)
        }
      })
    }
    else {
      Nil
    }
  }
}
