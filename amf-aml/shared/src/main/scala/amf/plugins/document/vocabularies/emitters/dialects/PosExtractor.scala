package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.metamodel.Field
import amf.core.model.domain.DomainElement
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.emitters.dialects.FieldEntryImplicit.FieldEntryWithPosition

trait PosExtractor {
  def fieldPos(element: DomainElement, field: Field): Position = {
    element.fields
      .entry(field)
      .flatMap(_.startPosition)
      .getOrElse(ZERO)
  }
}
