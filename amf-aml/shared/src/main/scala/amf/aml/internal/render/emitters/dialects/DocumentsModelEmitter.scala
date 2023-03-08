package amf.aml.internal.render.emitters.dialects

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{DocumentsModel, PublicNodeMapping}
import amf.aml.internal.metamodel.domain.DocumentMappingModel
import amf.aml.internal.render.emitters.instances.NodeMappableFinder
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.render.BaseEmitters.traverse
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import org.mulesoft.common.client.lexical.Position
import org.mulesoft.common.client.lexical.Position.ZERO
import org.yaml.model.YDocument.EntryBuilder

case class DocumentsModelEmitter(dialect: Dialect, ordering: SpecOrdering, aliases: Map[String, (String, String)])(
    implicit val nodeMappableFinder: NodeMappableFinder
) extends EntryEmitter
    with AliasesConsumer {
  val documents: DocumentsModel   = dialect.documents()
  var emitters: Seq[EntryEmitter] = Seq()

  override def emit(b: EntryBuilder): Unit = {
    b.entry(
      "documents",
      _.obj { b =>
        // Root Emitter
        if (Option(documents.root()).isDefined) {
          emitters ++= Seq(RootDocumentModelEmitter(dialect, ordering, aliases))
        }

        // Fragments emitter
        if (documents.fragments().nonEmpty) {
          emitters ++= Seq(FragmentsDocumentModelEmitter(dialect, ordering, aliases))
        }

        // Module emitter
        if (Option(documents.library()).isDefined) {
          emitters ++= Seq(LibraryDocumentModelEmitter(dialect, ordering, aliases))
        }

        emitters ++= Seq(DocumentsModelOptionsEmitter(dialect, ordering))

        traverse(ordering.sorted(emitters), b)
      }
    )
  }

  override def position(): Position = {
    val rootEncodedPosition: Seq[Position] = Option(documents.root())
      .flatMap { _ =>
        documents.root().fields.entry(DocumentMappingModel.EncodedNode).flatMap { enc =>
          enc.value.annotations.find(classOf[LexicalInformation]).map(_.range.start)
        }
      }
      .map(pos => Seq(pos))
      .getOrElse(Nil)

    val rootDeclared: Seq[PublicNodeMapping] = Option(documents.root()).map(_.declaredNodes()).getOrElse(Nil)
    val rootDeclaredPositions: Seq[Position] =
      rootDeclared.flatMap(_.annotations.find(classOf[LexicalInformation])).map(_.range.start)

    val libraryDeclarations: Seq[PublicNodeMapping] = Option(documents.library()).map(_.declaredNodes()).getOrElse(Nil)
    val libraryDeclarationPositions =
      libraryDeclarations.map(_.annotations.find(classOf[LexicalInformation]).map(_.range.start)).collect {
        case Some(pos) => pos
      }

    val fragmentPositions = documents.fragments().flatMap { fragment =>
      fragment.annotations.find(classOf[LexicalInformation]).map(_.range.start)
    }

    (rootEncodedPosition ++ rootDeclaredPositions ++ fragmentPositions ++ libraryDeclarationPositions).sorted.headOption
      .getOrElse(ZERO)
  }
}
