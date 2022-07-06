package amf.aml.internal.render.emitters.dialects

import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.annotations.LexicalInformation
import org.mulesoft.common.client.lexical.Position.ZERO

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
