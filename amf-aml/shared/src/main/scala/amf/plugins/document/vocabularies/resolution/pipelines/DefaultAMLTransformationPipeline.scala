package amf.plugins.document.vocabularies.resolution.pipelines

import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.resolution.stages.ResolutionStage
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, DialectInstancePatch}

class DefaultAMLTransformationPipeline() extends ResolutionPipeline {
  override def steps(implicit errorHandler: ErrorHandler): Seq[ResolutionStage] = Seq(new RedirectResolutionByModel())
}

private class RedirectResolutionByModel(override implicit val errorHandler: ErrorHandler) extends ResolutionStage {
  override def resolve[T <: BaseUnit](model: T): T =
    model match {
      case _: DialectInstancePatch => new DialectInstancePatchResolutionPipeline().transform(model, errorHandler)
      case _: Dialect              => new DialectResolutionPipeline().transform(model, errorHandler)
      case _: DialectInstance      => new DialectInstanceResolutionPipeline().transform(model, errorHandler)
      case _                       => model
    }
}
