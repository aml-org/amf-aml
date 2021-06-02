package amf.plugins.document.vocabularies.emitters.instances

import amf.core.emitter.{DomainElementEmitter, SpecOrdering}
import amf.client.remod.amfcore.config.RenderOptions
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.model.domain.DomainElement
import amf.plugins.document.vocabularies.annotations.DiscriminatorField
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.DialectDomainElement
import org.yaml.model.YNode

object AmlDomainElementEmitter extends DomainElementEmitter[Dialect] {

  /**
    * @param references : optional parameter which will improve emission of references defined in element
    */
  override def emit(element: DomainElement,
                    emissionStructure: Dialect,
                    eh: AMFErrorHandler,
                    references: Seq[BaseUnit] = Nil): YNode = {
    val partEmitter = element match {
      case element: DialectDomainElement => Some(dialectDomainElementEmitter(emissionStructure, references, element))
      case _                             => None
    }
    nodeOrError(partEmitter, element.id, eh)
  }

  private def dialectDomainElementEmitter(dialect: Dialect, references: Seq[BaseUnit], element: DialectDomainElement) = {
    val renderOptions = RenderOptions()
    val nodeMappable  = element.definedBy
    val discriminator = element.annotations.find(classOf[DiscriminatorField]).map(a => a.key -> a.value)
    val dialects      = references.collect { case dialect: Dialect => dialect }
    val finder        = DefaultNodeMappableFinder(dialects)
    DialectNodeEmitter(element,
                       nodeMappable,
                       references,
                       dialect,
                       SpecOrdering.Lexical,
                       discriminator = discriminator,
                       renderOptions = renderOptions)(finder)
  }
}
