package amf.aml.internal.render.emitters.dialects

import amf.core.internal.metamodel.Field
import amf.core.client.scala.model.domain.DomainElement
import amf.aml.internal.render.emitters.dialects.FieldEntryImplicit._
import org.mulesoft.common.client.lexical.Position
import org.mulesoft.common.client.lexical.Position.ZERO

trait PosExtractor {
  def fieldPos(element: DomainElement, field: Field): Position = {
    element.fields
      .entry(field)
      .flatMap(_.startPosition)
      .getOrElse(ZERO)
  }
}
