package amf.plugins.document.vocabularies.resolution.stages

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.metamodel.document.DocumentModel
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{DomainElement, Linkable}
import amf.core.client.scala.transform.stages.helpers.ModelReferenceResolver
import amf.core.client.scala.transform.stages.selectors.LinkSelector
import amf.core.client.scala.transform.stages.TransformationStep
import amf.plugins.document.vocabularies.model.document.DialectInstance

class DialectInstanceReferencesResolutionStage() extends TransformationStep {
  override def transform(model: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit = {
    new DialectInstanceReferencesResolution()(errorHandler).resolve(model)
  }
}

private class DialectInstanceReferencesResolution(implicit errorHandler: AMFErrorHandler) {
  var model: Option[BaseUnit]                       = None
  var modelResolver: Option[ModelReferenceResolver] = None
  var mutuallyRecursive: Seq[String]                = Nil

  def resolve[T <: BaseUnit](model: T): T = {
    this.model = Some(model)
    this.modelResolver = Some(new ModelReferenceResolver(model))
    model.transform(LinkSelector, transform).asInstanceOf[T]
  }

  // Internal request that checks for mutually recursive types
  protected def recursiveResolveInvocation(model: BaseUnit,
                                           modelResolver: Option[ModelReferenceResolver],
                                           mutuallyRecursive: Seq[String]): BaseUnit = {
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
        .recursiveResolveInvocation(nested, modelResolver, mutuallyRecursive ++ Seq(element.id))
      result.asInstanceOf[DialectInstance].encodes
    }
  }

}
