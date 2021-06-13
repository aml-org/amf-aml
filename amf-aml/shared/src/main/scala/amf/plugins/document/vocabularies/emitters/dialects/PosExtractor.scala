package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.internal.metamodel.Field
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.plugins.document.vocabularies.emitters.dialects.FieldEntryImplicit._

trait PosExtractor {
  def fieldPos(element: DomainElement, field: Field): Position = {
    element.fields
      .entry(field)
      .flatMap(_.startPosition)
      .getOrElse(ZERO)
  }
}
