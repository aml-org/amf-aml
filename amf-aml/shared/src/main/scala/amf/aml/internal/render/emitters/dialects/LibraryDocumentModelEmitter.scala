package amf.aml.internal.render.emitters.dialects

import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.render.BaseEmitters.{MapEntryEmitter, traverse}
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.aml.internal.render.emitters.instances.NodeMappableFinder
import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.DocumentMapping
import org.mulesoft.common.client.lexical.Position
import org.mulesoft.common.client.lexical.Position.ZERO
import org.yaml.model.YDocument.EntryBuilder

case class LibraryDocumentModelEmitter(
    dialect: Dialect,
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)]
)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AliasesConsumer {
  val mapping: DocumentMapping    = dialect.documents().library()
  var emitters: Seq[EntryEmitter] = Seq()

  override def emit(b: EntryBuilder): Unit = {
    val declaredNodes = mapping
      .declaredNodes()
      .map { declaration =>
        aliasFor(declaration.mappedNode().value()) match {
          case Some(declaredId) => MapEntryEmitter(declaration.name().value(), declaredId)
          case _                => MapEntryEmitter(declaration.name().value(), declaration.mappedNode().value())
        }
      }
    val sortedNodes = ordering.sorted(declaredNodes)
    emitters ++= Seq(new EntryEmitter {

      override def emit(b: EntryBuilder): Unit = {
        b.entry(
          "declares",
          _.obj { b =>
            traverse(sortedNodes, b)
          }
        )
      }

      override def position(): Position = sortedNodes.head.position
    })

    b.entry(
      "library",
      _.obj { b =>
        traverse(ordering.sorted(emitters), b)
      }
    )
  }

  override def position(): Position = {
    val allPos = dialect
      .documents()
      .library()
      .declaredNodes()
      .map { lib =>
        lib.annotations.find(classOf[LexicalInformation]).map(_.range.start)
      }
      .collect { case Some(pos) => pos }
    allPos.sorted.headOption.getOrElse(ZERO)
  }
}
