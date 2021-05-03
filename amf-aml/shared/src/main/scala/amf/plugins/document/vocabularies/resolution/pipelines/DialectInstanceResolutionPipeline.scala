package amf.plugins.document.vocabularies.resolution.pipelines

import amf.core.errorhandling.ErrorHandler
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.resolution.stages.{CleanReferencesStage, DeclarationsRemovalStage, ResolutionStage}
import amf.plugins.document.vocabularies.resolution.stages.DialectInstanceReferencesResolutionStage
import amf.{AmfProfile, ProfileName}

class DialectInstanceResolutionPipeline private (override val name: String) extends ResolutionPipeline() {

  override def steps(implicit eh: ErrorHandler): Seq[ResolutionStage] =
    Seq(
        new DialectInstanceReferencesResolutionStage(),
        new CleanReferencesStage(),
        new DeclarationsRemovalStage()
    )

}

object DialectInstanceResolutionPipeline {
  val name: String = "DialectInstanceResolutionPipeline"
  def apply()      = new DialectInstanceResolutionPipeline(name)
}
