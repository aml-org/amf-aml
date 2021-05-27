package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.LexicalInformation
import amf.core.model.domain.DomainElement
import amf.core.parser.Position.ZERO

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
