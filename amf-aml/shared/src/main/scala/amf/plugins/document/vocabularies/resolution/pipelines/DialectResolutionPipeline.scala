package amf.plugins.document.vocabularies.resolution.pipelines

import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.resolution.stages.ResolutionStage
import amf.plugins.document.vocabularies.resolution.stages.{
  DialectNodeExtensionStage,
  DialectReferencesResolutionStage
}
import amf.{AmfProfile, ProfileName}

class DialectResolutionPipeline() extends ResolutionPipeline() {

  override def steps(model: BaseUnit, sourceVendor: String)(
      implicit errorHandler: ErrorHandler): Seq[ResolutionStage] =
    Seq(new DialectReferencesResolutionStage(), new DialectNodeExtensionStage())
}
