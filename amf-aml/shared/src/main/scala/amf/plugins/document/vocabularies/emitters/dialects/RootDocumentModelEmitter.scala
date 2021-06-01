package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.LexicalInformation
import amf.core.emitter.BaseEmitters.{MapEntryEmitter, traverse}
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.emitters.instances.NodeMappableFinder
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.DocumentMapping
import org.yaml.model.YDocument.EntryBuilder

case class RootDocumentModelEmitter(dialect: Dialect, ordering: SpecOrdering, aliases: Map[String, (String, String)])(
    implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AliasesConsumer {
  val mapping: DocumentMapping    = dialect.documents().root()
  var emitters: Seq[EntryEmitter] = Seq()

  override def emit(b: EntryBuilder): Unit = {
    mapping.encoded().option().foreach { encodedId =>
      aliasFor(encodedId) match {
        case Some(alias) => emitters ++= Seq(MapEntryEmitter("encodes", alias))
        case None        => emitters ++= Seq(MapEntryEmitter("encodes", encodedId))
      }
    }
    val decls = mapping.declaredNodes()
    if (decls.nonEmpty) {
      val declaredNodes = decls
        .map { declaration =>
          aliasFor(declaration.mappedNode().value()) match {
            case Some(declaredId) => MapEntryEmitter(declaration.name().value(), declaredId)
            case _                => MapEntryEmitter(declaration.name().value(), declaration.mappedNode().value())
          }
        }
      val sortedNodes = ordering.sorted(declaredNodes)
      emitters ++= Seq(new EntryEmitter {

        override def emit(b: EntryBuilder): Unit = {
          b.entry("declares", _.obj { b =>
            traverse(sortedNodes, b)
          })
        }

        override def position(): Position = {
          sortedNodes.head.position
        }
      })
    }
    b.entry("root", _.obj { b =>
      traverse(ordering.sorted(emitters), b)
    })
  }

  override def position(): Position =
    dialect.documents().root().annotations.find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)
}
