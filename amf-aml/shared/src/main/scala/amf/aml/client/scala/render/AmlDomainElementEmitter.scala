package amf.aml.client.scala.render

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.DialectDomainElement
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.DomainElementEmitter
import amf.aml.internal.annotations.DiscriminatorField
import amf.aml.internal.render.emitters.instances.{DefaultNodeMappableFinder, DialectNodeEmitter}
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

  private def dialectDomainElementEmitter(dialect: Dialect,
                                          references: Seq[BaseUnit],
                                          element: DialectDomainElement) = {
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
