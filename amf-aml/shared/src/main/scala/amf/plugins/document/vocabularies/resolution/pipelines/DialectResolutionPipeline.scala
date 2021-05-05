package amf.plugins.document.vocabularies.resolution.pipelines

import amf.core.errorhandling.ErrorHandler
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.TransformationStep
import amf.plugins.document.vocabularies.resolution.stages.{
  DialectNodeExtensionStage,
  DialectReferencesResolutionStage
}
import amf.{AmfProfile, ProfileName}

class DialectResolutionPipeline private (override val name: String) extends TransformationPipeline() {

  override def steps: Seq[TransformationStep] =
    Seq(new DialectReferencesResolutionStage(), new DialectNodeExtensionStage())

}

object DialectResolutionPipeline {
  val name: String = "DialectResolutionPipeline"
  def apply()      = new DialectResolutionPipeline(name)
}
