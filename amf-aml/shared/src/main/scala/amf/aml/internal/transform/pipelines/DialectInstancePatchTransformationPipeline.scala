package amf.aml.internal.transform.pipelines
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.transform.stages.{CleanReferencesStage, DeclarationsRemovalStage}
import amf.aml.internal.transform.steps.{DialectInstanceReferencesResolutionStage, DialectPatchApplicationStage}
import amf.core.client.scala.transform.{TransformationPipeline, TransformationStep}

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
