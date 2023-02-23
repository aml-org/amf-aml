package amf.aml.internal.render.emitters.dialects

import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.aml.internal.render.emitters.instances.NodeMappableFinder
import amf.aml.client.scala.model.document.Dialect
import org.mulesoft.common.client.lexical.Position
import org.mulesoft.common.client.lexical.Position.ZERO
import org.yaml.model.YDocument.EntryBuilder

case class FragmentsDocumentModelEmitter(
    dialect: Dialect,
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)]
)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AliasesConsumer {
  var emitters: Seq[EntryEmitter] = dialect.documents().fragments().map { fragmentMapping =>
    FragmentMappingEmitter(dialect, fragmentMapping, ordering, aliases)
  }

  override def emit(b: EntryBuilder): Unit = {

    b.entry(
      "fragments",
      _.obj { b =>
        b.entry(
          "encodes",
          _.obj { b =>
            ordering.sorted(emitters).foreach(_.emit(b))
          }
        )
      }
    )
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
