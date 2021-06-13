package amf.plugins.document.vocabularies.resolution.pipelines
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.transform.pipelines.TransformationPipeline
import amf.core.client.scala.transform.stages.{CleanReferencesStage, DeclarationsRemovalStage, TransformationStep}
import amf.plugins.document.vocabularies.resolution.stages.{
  DialectInstanceReferencesResolutionStage,
  DialectPatchApplicationStage
}

class DialectInstancePatchTransformationPipeline private (override val name: String) extends TransformationPipeline() {

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
