package amf.plugins.document.vocabularies.emitters.instances

import amf.core.emitter.{RenderOptions, SpecOrdering}
import amf.core.model.domain.DomainElement
import amf.plugins.document.vocabularies.annotations.DiscriminatorField
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceUnit}
import amf.plugins.document.vocabularies.model.domain.DialectDomainElement
import org.yaml.model.{YDocument, YNode}

object DomainElementEmitter {

  def emit(element: DomainElement, instance: DialectInstanceUnit, dialect: Dialect): YNode = {
    val partEmitter = element match {
      case element: DialectDomainElement => dialectDomainElementEmitter(instance, dialect, element)
      case _ => None
    }
    partEmitter
      .map { emitter =>
        YDocument(b => emitter.emit(b)).node
      }
      .getOrElse {
        YNode.Empty
      }

  }

  private def dialectDomainElementEmitter(instance: DialectInstanceUnit, dialect: Dialect, element: DialectDomainElement) = {
    val renderOptions = RenderOptions()
    val nodeMappable = element.definedBy
    val discriminator = element.annotations.find(classOf[DiscriminatorField]).map(a => a.key -> a.value)
    Some(DialectNodeEmitter(element, nodeMappable, instance, dialect, SpecOrdering.Lexical, discriminator = discriminator, renderOptions = renderOptions))
  }
}
