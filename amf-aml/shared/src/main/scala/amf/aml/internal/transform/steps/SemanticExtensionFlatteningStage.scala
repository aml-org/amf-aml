package amf.aml.internal.transform.steps

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.transform.TransformationStep
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel._
import amf.core.internal.parser.domain.FieldEntry

class SemanticExtensionFlatteningStage() extends TransformationStep {

  private lazy val filterFields = DomainExtensionModel.fields

  override def transform(
      model: BaseUnit,
      errorHandler: AMFErrorHandler,
      configuration: AMFGraphConfiguration
  ): BaseUnit = {
    model.iterator().foreach {
      case element: DomainElement if hasSemanticExtension(element) => compactExtensionsIntoElement(element)
      case _                                                       => // Ignore
    }
    model
  }

  private def hasSemanticExtension(element: DomainElement): Boolean = {
    element.customDomainProperties.exists(isSemanticExtension)
  }

  private def isSemanticExtension(extension: DomainExtension): Boolean = Option(extension.extension).isEmpty

  private def compactExtensionsIntoElement(element: DomainElement): DomainElement = {
    element.customDomainProperties.filter(isSemanticExtension).foldLeft(element) { (elem, extension) =>
      compactableFields(extension).foldLeft(elem) { (elem, fieldEntry) =>
        elem.set(fieldEntry.field, fieldEntry.element)
      }
    }
    val notSemanticExtensions = element.customDomainProperties.filterNot(isSemanticExtension)
    if (notSemanticExtensions.isEmpty) {
      element.fields.removeField(CustomDomainProperties)
      element
    } else element.withCustomDomainProperties(notSemanticExtensions)
  }

  private def compactableFields(extension: DomainExtension): Iterable[FieldEntry] = {
    extension.fields.fields().filter(p => !filterFields.contains(p.field))
  }
}
