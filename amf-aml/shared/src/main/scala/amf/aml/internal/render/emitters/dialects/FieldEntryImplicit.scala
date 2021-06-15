package amf.aml.internal.render.emitters.dialects

import amf.core.client.common.position.Position
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.parser.domain.FieldEntry

private object FieldEntryImplicit {
  implicit class FieldEntryWithPosition(entry: FieldEntry) {
    def startPosition: Option[Position] =
      entry.value.annotations
        .find(classOf[LexicalInformation])
        .map(_.range.start)
  }
}
