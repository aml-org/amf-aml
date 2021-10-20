package amf.aml.internal.transform.steps

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.transform.TransformationStep
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel._

object SemanticExtensionFlatteningStage extends TransformationStep {

  private lazy val filterFields = DomainExtensionModel.fields

  override def transform(model: BaseUnit,
                         errorHandler: AMFErrorHandler,
                         configuration: AMFGraphConfiguration): BaseUnit = {
    model.transform(hasSemanticExtension, transform)(errorHandler)
  }

  private def hasSemanticExtension(element: DomainElement): Boolean = {
    element.customDomainProperties.exists(isSemanticExtension)
  }

  private def isSemanticExtension(extension: DomainExtension): Boolean = Option(extension.extension).isEmpty

  private def transform(element: DomainElement, isCycle: Boolean): Option[DomainElement] = {
    Some(compactExtensionsIntoElement(element))
  }

  private def compactExtensionsIntoElement(element: DomainElement): DomainElement = {
    element.customDomainProperties.filter(isSemanticExtension).foldLeft(element) { (elem, extension) =>
      compactableFields(extension).foldLeft(elem) { (elem, fieldEntry) =>
        elem.set(fieldEntry.field, fieldEntry.element)
      }
    }
  }

  private def compactableFields(extension: DomainExtension) = {
    extension.fields.fields().filter(p => !filterFields.contains(p.field))
  }
}
