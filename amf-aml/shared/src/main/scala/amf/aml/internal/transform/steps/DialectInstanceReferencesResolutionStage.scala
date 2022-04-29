package amf.aml.internal.transform.steps

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.metamodel.document.DocumentModel
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{DomainElement, Linkable}
import amf.core.internal.transform.stages.helpers.ModelReferenceResolver
import amf.core.internal.transform.stages.selectors.LinkSelector
import amf.aml.client.scala.model.document.DialectInstance
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.transform.TransformationStep

class DialectInstanceReferencesResolutionStage() extends TransformationStep {
  override def transform(
      model: BaseUnit,
      errorHandler: AMFErrorHandler,
      configuration: AMFGraphConfiguration
  ): BaseUnit = {
    new DialectInstanceReferencesResolution()(errorHandler).transform(model)
  }
}

private class DialectInstanceReferencesResolution(implicit errorHandler: AMFErrorHandler) {
  var model: Option[BaseUnit]                       = None
  var modelResolver: Option[ModelReferenceResolver] = None
  var mutuallyRecursive: Seq[String]                = Nil

  def transform[T <: BaseUnit](model: T): T = {
    this.model = Some(model)
    this.modelResolver = Some(new ModelReferenceResolver(model))
    model.transform(LinkSelector, transform).asInstanceOf[T]
  }

  // Internal request that checks for mutually recursive types
  protected def recursiveTransformInvocation(
      model: BaseUnit,
      modelResolver: Option[ModelReferenceResolver],
      mutuallyRecursive: Seq[String]
  ): BaseUnit = {
    this.mutuallyRecursive = mutuallyRecursive
    this.model = Some(model)
    this.modelResolver = Some(modelResolver.getOrElse(new ModelReferenceResolver(model)))
    model.transform(LinkSelector, transform)
  }

  def transform(element: DomainElement, isCycle: Boolean): Option[DomainElement] = {
    element match {

      // link not traversed, cache it and traverse it
      case l: Linkable if l.linkTarget.isDefined && !isCycle => Some(resolveLinked(l.linkTarget.get))

      // link traversed, return the link
      case l: Linkable if l.linkTarget.isDefined => Some(l)

      // no link
      case other => Some(other)

    }
  }

  def resolveLinked(element: DomainElement): DomainElement = {
    if (mutuallyRecursive.contains(element.id)) {
      element
    } else {
      val nested = DialectInstance()
      nested.fields.setWithoutId(DocumentModel.Encodes, element)
      val result = new DialectInstanceReferencesResolution()
        .recursiveTransformInvocation(nested, modelResolver, mutuallyRecursive ++ Seq(element.id))
      result.asInstanceOf[DialectInstance].encodes
    }
  }

}
