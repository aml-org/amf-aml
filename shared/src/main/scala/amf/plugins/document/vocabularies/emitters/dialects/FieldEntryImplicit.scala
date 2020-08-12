package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.LexicalInformation
import amf.core.parser.{FieldEntry, Position}

private object FieldEntryImplicit {
  implicit class FieldEntryWithPosition(entry: FieldEntry) {
    def startPosition: Option[Position] =
      entry.value.annotations
        .find(classOf[LexicalInformation])
        .map(_.range.start)
  }
}
