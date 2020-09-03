package amf.plugins.document.vocabularies.emitters.instances

import amf.core.emitter.{DomainElementEmitterHelper, RenderOptions, SpecOrdering}
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.model.domain.DomainElement
import amf.plugins.document.vocabularies.annotations.DiscriminatorField
import amf.plugins.document.vocabularies.model.document.{Dialect}
import amf.plugins.document.vocabularies.model.domain.DialectDomainElement
import org.yaml.model.YNode

object DomainElementEmitter extends DomainElementEmitterHelper {

  def emit(element: DomainElement, dialect: Dialect, eh: ErrorHandler, references: Seq[BaseUnit] = Nil): YNode = {
    val partEmitter = element match {
      case element: DialectDomainElement => Some(dialectDomainElementEmitter(dialect, references, element))
      case _ => None
    }
    nodeOrError(partEmitter, element.id, eh)
  }

  private def dialectDomainElementEmitter(dialect: Dialect, references: Seq[BaseUnit], element: DialectDomainElement) = {
    val renderOptions = RenderOptions()
    val nodeMappable = element.definedBy
    val discriminator = element.annotations.find(classOf[DiscriminatorField]).map(a => a.key -> a.value)
    DialectNodeEmitter(element, nodeMappable, references, dialect, SpecOrdering.Lexical, discriminator = discriminator, renderOptions = renderOptions)
  }
}
