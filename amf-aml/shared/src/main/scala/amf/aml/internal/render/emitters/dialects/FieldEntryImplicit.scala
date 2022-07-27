package amf.aml.internal.render.emitters.dialects

import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.parser.domain.FieldEntry
import org.mulesoft.common.client.lexical.Position

private object FieldEntryImplicit {
  implicit class FieldEntryWithPosition(entry: FieldEntry) {
    def startPosition: Option[Position] =
      entry.value.annotations
        .find(classOf[LexicalInformation])
        .map(_.range.start)
  }
}
