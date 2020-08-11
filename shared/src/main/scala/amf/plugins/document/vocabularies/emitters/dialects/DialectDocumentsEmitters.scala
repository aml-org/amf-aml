package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.{Aliases, LexicalInformation}
import amf.core.emitter.BaseEmitters.traverse
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.model.document.DeclaresModel
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.emitters.common.{ExternalEmitter, IdCounter}
import amf.plugins.document.vocabularies.model.document.{Dialect, Vocabulary}
import amf.plugins.document.vocabularies.model.domain.{External, NodeMappable}
import org.yaml.model.YDocument.EntryBuilder

trait DialectDocumentsEmitters {

  val dialect: Dialect
  val aliases: Map[String, (String, String)]

  def collectAliases(): Map[String, (Aliases.FullUrl, Aliases.Alias)] = {
    val vocabFile       = dialect.location().getOrElse(dialect.id).split("/").last
    val vocabFilePrefix = dialect.location().getOrElse(dialect.id).replace(vocabFile, "")

    val maps = dialect.annotations
      .find(classOf[Aliases])
      .map { aliases =>
        aliases.aliases.foldLeft(Map[String, String]()) {
          case (acc, (alias, (id, _))) =>
            acc + (id -> alias)
        }
      }
      .getOrElse(Map())
    val idCounter = new IdCounter()
    val dialectReferences = dialect.references.foldLeft(Map[String, (String, String)]()) {
      case (acc: Map[String, (String, String)], m: DeclaresModel) =>
        val importLocation: String = if (m.location().exists(_.contains(vocabFilePrefix))) {
          m.location().getOrElse(m.id).replace(vocabFilePrefix, "")
        }
        else {
          m.location().getOrElse(m.id).replace("file://", "")
        }

        val aliasKey = m match {
          case v: Vocabulary => v.base.value()
          case _             => m.id
        }
        if (maps.get(aliasKey).isDefined) {
          val alias = maps(aliasKey)
          acc + (aliasKey -> (alias, importLocation))
        }
        else {
          val nextAlias = idCounter.genId("uses_")
          acc + (aliasKey -> (nextAlias, importLocation))
        }
      case (acc: Map[String, (String, String)], _) => acc
    }
    dialect.externals.foldLeft(dialectReferences) {
      case (acc: Map[String, (String, String)], e: External) =>
        acc + (e.base.value() -> (e.alias.value(), ""))
    }
  }

  def rootLevelEmitters(ordering: SpecOrdering): Seq[EntryEmitter] =
    Seq(ReferencesEmitter(dialect, ordering, aliases)) ++
      nodeMappingDeclarationEmitters(dialect, ordering, aliases) ++
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
      case nm: NodeMappable => nm
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
}
