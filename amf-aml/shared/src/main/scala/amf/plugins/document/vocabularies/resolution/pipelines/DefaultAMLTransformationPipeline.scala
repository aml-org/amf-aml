package amf.plugins.document.vocabularies.resolution.pipelines

import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.{BasicResolutionPipeline, ResolutionPipeline}
import amf.core.resolution.stages.ResolutionStage
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, DialectInstancePatch}

class DefaultAMLTransformationPipeline() extends ResolutionPipeline {

  override def steps(model: BaseUnit, sourceVendor: String)(
      implicit errorHandler: ErrorHandler): Seq[ResolutionStage] = {
    model match {
      case _: DialectInstancePatch => new DialectInstancePatchResolutionPipeline().steps(model, sourceVendor)
      case _: Dialect              => new DialectResolutionPipeline().steps(model, sourceVendor)
      case _: DialectInstance      => new DialectInstanceResolutionPipeline().steps(model, sourceVendor)
      case _                       => new BasicResolutionPipeline().steps(model, sourceVendor)
    }
  }

}
