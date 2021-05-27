package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.Aliases.{Alias, FullUrl, ImportLocation}
import amf.core.emitter.BaseEmitters._
import amf.core.emitter.SpecOrdering.Lexical
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.emitters.dialects.FieldEntryImplicit._
import amf.plugins.document.vocabularies.emitters.instances.NodeMappableFinder
import amf.plugins.document.vocabularies.metamodel.document.DialectModel
import amf.plugins.document.vocabularies.model.document.Dialect
import org.yaml.model.YDocument
import org.yaml.model.YDocument.EntryBuilder

case class DialectEmitter(dialect: Dialect)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends DialectDocumentsEmitters {

  val ordering: SpecOrdering = Lexical
  val aliases: Map[RefKey, (Alias, ImportLocation)] =
    buildReferenceAliasIndexFrom(dialect) ++ buildExternalsAliasIndexFrom(dialect)

  def emitDialect(): YDocument = {
    val content: Seq[EntryEmitter] = rootLevelEmitters(ordering) ++ dialectEmitters(ordering)

    YDocument(b => {
      b.comment("%Dialect 1.0")
      b.obj { b =>
        traverse(ordering.sorted(content), b)
      }
    })
  }

  def dialectEmitters(ordering: SpecOrdering): Seq[EntryEmitter] =
    dialectPropertiesEmitter(ordering)

  def dialectPropertiesEmitter(ordering: SpecOrdering): Seq[EntryEmitter] = {
    var emitters: Seq[EntryEmitter] = Nil

    emitters ++= Seq(new EntryEmitter {
      override def emit(b: EntryBuilder): Unit = {
        MapEntryEmitter("dialect", dialect.name().value()).emit(b)
      }

      override def position(): Position =
        dialect.fields
          .entry(DialectModel.Name)
          .flatMap(_.startPosition)
          .getOrElse(ZERO)

    })

    emitters ++= Seq(new EntryEmitter {
      override def emit(b: EntryBuilder): Unit =
        MapEntryEmitter("version", dialect.version().value()).emit(b)

      override def position(): Position =
        dialect.fields
          .entry(DialectModel.Version)
          .flatMap(_.startPosition)
          .getOrElse(ZERO)
    })

    if (dialect.usage.nonEmpty) {
      emitters ++= Seq(new EntryEmitter {
        override def emit(b: EntryBuilder): Unit = MapEntryEmitter("usage", dialect.usage.value()).emit(b)

        override def position(): Position =
          dialect.fields
            .entry(DialectModel.Usage)
            .flatMap(_.startPosition)
            .getOrElse(ZERO)
      })
    }

    if (Option(dialect.documents()).isDefined) {
      emitters ++= Seq(DocumentsModelEmitter(dialect, ordering, aliases))
    }

    emitters
  }
}
