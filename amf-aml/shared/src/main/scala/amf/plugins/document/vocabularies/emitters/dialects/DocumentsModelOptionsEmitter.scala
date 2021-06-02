package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.emitter.BaseEmitters.{MapEntryEmitter, pos, traverse}
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.emitters.instances.NodeMappableFinder
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.DocumentsModel
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YType

case class DocumentsModelOptionsEmitter(
    dialect: Dialect,
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)] = Map())(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AliasesConsumer {

  val mapping: DocumentsModel     = dialect.documents()
  var emitters: Seq[EntryEmitter] = Seq()

  private def hasOptions: Boolean =
    Seq(mapping.selfEncoded().option(), mapping.declarationsPath().option(), mapping.keyProperty().option()).flatten.nonEmpty

  val sortedNodes: Seq[MapEntryEmitter] = if (hasOptions) {
    val options =
      Map(
          "selfEncoded"      -> mapping.selfEncoded().option(),
          "declarationsPath" -> mapping.declarationsPath().option(),
          "keyProperty"      -> mapping.keyProperty().option(),
          "referenceStyle"   -> mapping.referenceStyle().option()
      )
    val types = Map("selfEncoded" -> YType.Bool,
                    "keyProperty"      -> YType.Bool,
                    "declarationsPath" -> YType.Str,
                    "referenceStyle"   -> YType.Str)
    val annotations = Map(
        "selfEncoded"      -> mapping.selfEncoded().annotations(),
        "declarationsPath" -> mapping.declarationsPath().annotations(),
        "keyProperty"      -> mapping.keyProperty().annotations(),
        "referenceStyle"   -> mapping.referenceStyle().annotations()
    )

    val optionNodes: Seq[MapEntryEmitter] = options
      .map {
        case (optionName, maybeValue) =>
          maybeValue map { value =>
            val key                = optionName
            val nodetype           = types(optionName)
            val position: Position = pos(annotations(optionName))
            MapEntryEmitter(optionName, value.toString, nodetype, position)
          }
      }
      .collect({ case Some(node) => node })
      .toSeq
    val sorted: Seq[MapEntryEmitter] = ordering.sorted(optionNodes)
    sorted
  } else
    Nil

  override def emit(b: EntryBuilder): Unit = {
    if (sortedNodes.nonEmpty) {
      b.entry("options", _.obj { b =>
        traverse(sortedNodes, b)
      })
    }
  }

  override def position(): Position = sortedNodes.headOption.map(_.position).getOrElse(ZERO)
}
