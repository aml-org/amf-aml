package amf.plugins.document.vocabularies.resolution.pipelines

import amf.core.errorhandling.AMFErrorHandler
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.{CleanReferencesStage, DeclarationsRemovalStage, TransformationStep}
import amf.plugins.document.vocabularies.resolution.stages.DialectInstanceReferencesResolutionStage
import amf.{AmfProfile, ProfileName}

class DialectInstanceTransformationPipeline private (override val name: String) extends TransformationPipeline() {

  override def steps: Seq[TransformationStep] =
    Seq(
        new DialectInstanceReferencesResolutionStage(),
        new CleanReferencesStage(),
        new DeclarationsRemovalStage()
    )

}

object DialectInstanceTransformationPipeline {
  val name: String = "DialectInstanceTransformationPipeline"
  def apply()      = new DialectInstanceTransformationPipeline(name)
}
