package amf.aml.internal.render.emitters.dialects

import amf.core.internal.annotations.Aliases.{Alias, FullUrl, ImportLocation}
import amf.core.internal.render.BaseEmitters.{MapEntryEmitter, traverse}
import amf.core.internal.render.SpecOrdering.Lexical
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.aml.internal.metamodel.document.DialectModel
import amf.aml.client.scala.model.document.{Dialect, DialectLibrary}
import org.yaml.model.YDocument
import org.yaml.model.YDocument.EntryBuilder
import amf.aml.internal.render.emitters.dialects.FieldEntryImplicit.FieldEntryWithPosition
import amf.aml.internal.render.emitters.instances.NodeMappableFinder

case class RamlDialectLibraryEmitter(library: DialectLibrary)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends DialectDocumentsEmitters {

  val ordering: SpecOrdering    = Lexical
  override val dialect: Dialect = toDialect(library)
  val aliases
    : Map[RefKey, (Alias, ImportLocation)] = buildReferenceAliasIndexFrom(dialect) ++ buildExternalsAliasIndexFrom(
      dialect)

  def emitDialectLibrary(): YDocument = {
    val content: Seq[EntryEmitter] = rootLevelEmitters(ordering) ++ dialectEmitters(ordering)

    YDocument(b => {
      b.comment("%Library / Dialect 1.0")
      b.obj { b =>
        traverse(ordering.sorted(content), b)
      }
    })
  }

  protected def toDialect(library: DialectLibrary): Dialect = {
    val dialect = Dialect(library.fields, library.annotations).withId(library.id)
    dialect.processingData.adopted(library.id + "#")
    dialect
  }

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
