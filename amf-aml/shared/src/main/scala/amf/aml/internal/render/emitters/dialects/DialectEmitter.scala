package amf.aml.internal.render.emitters.dialects

import amf.core.internal.annotations.Aliases.{Alias, FullUrl, ImportLocation}
import amf.core.internal.render.BaseEmitters._
import amf.core.internal.render.SpecOrdering.Lexical
import amf.core.internal.render.emitters.EntryEmitter
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.internal.render.SpecOrdering
import amf.aml.internal.render.emitters.dialects.FieldEntryImplicit._
import amf.aml.internal.render.emitters.instances.NodeMappableFinder
import amf.aml.internal.metamodel.document.DialectModel
import amf.aml.client.scala.model.document.Dialect
import org.yaml.model.YDocument
import org.yaml.model.YDocument.EntryBuilder

trait DocumentCreator {
  def apply(content: Seq[EntryEmitter]): YDocument
}

case class DialectEmitter(dialect: Dialect, document: DocumentCreator)(implicit
    val nodeMappableFinder: NodeMappableFinder
) extends DialectDocumentsEmitters {

  val ordering: SpecOrdering = Lexical
  val aliases: Map[RefKey, (Alias, ImportLocation)] =
    buildReferenceAliasIndexFrom(dialect) ++ buildExternalsAliasIndexFrom(dialect)

  def emitDialect(): YDocument = {
    val content: Seq[EntryEmitter] = rootLevelEmitters(ordering) ++ dialectEmitters(ordering)
    document(ordering.sorted(content))
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
