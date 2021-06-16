package amf.aml.internal.render.emitters.dialects

import amf.core.internal.annotations.LexicalInformation
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.common.position.Position.ZERO

trait GroupPosition {
  def groupPosition(elements: Seq[DomainElement]) =
    elements
      .map(lexicalStartOrZero)
      .filter(_ != ZERO)
      .sorted
      .headOption
      .getOrElse(ZERO)

  private def lexicalStartOrZero(element: DomainElement) =
    element.annotations
      .find(classOf[LexicalInformation])
      .map(_.range.start)
      .getOrElse(ZERO)
}
