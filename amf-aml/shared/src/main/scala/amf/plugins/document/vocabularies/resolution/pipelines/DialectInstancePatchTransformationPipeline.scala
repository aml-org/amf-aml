package amf.plugins.document.vocabularies.resolution.pipelines
import amf.core.errorhandling.ErrorHandler
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.{CleanReferencesStage, DeclarationsRemovalStage, TransformationStep}
import amf.plugins.document.vocabularies.resolution.stages.{
  DialectInstanceReferencesResolutionStage,
  DialectPatchApplicationStage
}
import amf.{AmfProfile, ProfileName}

class DialectInstancePatchTransformationPipeline private(override val name: String) extends TransformationPipeline() {

  override def steps: Seq[TransformationStep] =
    Seq(
        new DialectInstanceReferencesResolutionStage(),
        new DialectPatchApplicationStage(),
        new CleanReferencesStage(),
        new DeclarationsRemovalStage()
    )

}

object DialectInstancePatchTransformationPipeline {
  val name: String = "DialectInstancePatchTransformationPipeline"
  def apply()      = new DialectInstancePatchTransformationPipeline(name)
}
