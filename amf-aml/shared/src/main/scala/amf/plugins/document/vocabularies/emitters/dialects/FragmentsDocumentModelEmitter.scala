package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.emitters.instances.NodeMappableFinder
import amf.plugins.document.vocabularies.model.document.Dialect
import org.yaml.model.YDocument.EntryBuilder

case class FragmentsDocumentModelEmitter(
    dialect: Dialect,
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)])(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AliasesConsumer {
  var emitters: Seq[EntryEmitter] = dialect.documents().fragments().map { fragmentMapping =>
    FragmentMappingEmitter(dialect, fragmentMapping, ordering, aliases)
  }

  override def emit(b: EntryBuilder): Unit = {

    b.entry("fragments", _.obj { b =>
      b.entry("encodes", _.obj { b =>
        ordering.sorted(emitters).foreach(_.emit(b))
      })
    })
  }

  override def position(): Position =
    ordering
      .sorted(emitters)
      .headOption
      .map { e: EntryEmitter =>
        e.position()
      }
      .getOrElse(ZERO)
}
