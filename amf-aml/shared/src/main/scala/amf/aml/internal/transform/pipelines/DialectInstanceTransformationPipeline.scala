package amf.aml.internal.transform.pipelines

import amf.core.client.scala.transform.pipelines.TransformationPipeline
import amf.core.client.scala.transform.stages.{CleanReferencesStage, DeclarationsRemovalStage, TransformationStep}
import amf.aml.internal.transform.steps.DialectInstanceReferencesResolutionStage

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
