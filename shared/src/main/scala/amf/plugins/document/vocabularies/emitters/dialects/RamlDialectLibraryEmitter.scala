package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.Aliases.{Alias, FullUrl, ImportLocation}
import amf.core.emitter.BaseEmitters.{MapEntryEmitter, traverse}
import amf.core.emitter.SpecOrdering.Lexical
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.metamodel.document.DialectModel
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectLibrary}
import org.yaml.model.YDocument
import org.yaml.model.YDocument.EntryBuilder
import amf.plugins.document.vocabularies.emitters.dialects.FieldEntryImplicit.FieldEntryWithPosition

case class RamlDialectLibraryEmitter(library: DialectLibrary) extends DialectDocumentsEmitters {

  val ordering: SpecOrdering                        = Lexical
  override val dialect: Dialect                     = toDialect(library)
  val aliases: Map[RefKey, (Alias, ImportLocation)] = buildReferenceAliasIndexFrom(dialect) ++ buildExternalsAliasIndexFrom(dialect)

  def emitDialectLibrary(): YDocument = {
    val content: Seq[EntryEmitter] = rootLevelEmitters(ordering) ++ dialectEmitters(ordering)

    YDocument(b => {
      b.comment("%Library / Dialect 1.0")
      b.obj { b =>
        traverse(ordering.sorted(content), b)
      }
    })
  }

  protected def toDialect(library: DialectLibrary): Dialect =
    Dialect(library.fields, library.annotations).withId(library.id)

  def dialectEmitters(ordering: SpecOrdering): Seq[EntryEmitter] =
    dialectPropertiesEmitter(ordering)

  def dialectPropertiesEmitter(ordering: SpecOrdering): Seq[EntryEmitter] = {
    var emitters: Seq[EntryEmitter] = Nil

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
    emitters
  }

}
