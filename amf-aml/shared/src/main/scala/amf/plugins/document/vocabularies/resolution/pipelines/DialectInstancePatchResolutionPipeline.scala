package amf.plugins.document.vocabularies.resolution.pipelines
import amf.core.errorhandling.ErrorHandler
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.resolution.stages.{CleanReferencesStage, DeclarationsRemovalStage, ResolutionStage}
import amf.plugins.document.vocabularies.resolution.stages.{
  DialectInstanceReferencesResolutionStage,
  DialectPatchApplicationStage
}
import amf.{AmfProfile, ProfileName}

class DialectInstancePatchResolutionPipeline private (override val name: String) extends ResolutionPipeline() {

  override def steps(implicit eh: ErrorHandler): Seq[ResolutionStage] =
    Seq(
        new DialectInstanceReferencesResolutionStage(),
        new DialectPatchApplicationStage(),
        new CleanReferencesStage(),
        new DeclarationsRemovalStage()
    )

}

object DialectInstancePatchResolutionPipeline {
  val name: String = "DialectInstancePatchResolutionPipeline"
  def apply()      = new DialectInstancePatchResolutionPipeline(name)
}
