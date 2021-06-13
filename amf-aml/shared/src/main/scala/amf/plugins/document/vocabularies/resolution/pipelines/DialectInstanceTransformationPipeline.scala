package amf.plugins.document.vocabularies.resolution.pipelines

import amf.core.client.scala.transform.pipelines.TransformationPipeline
import amf.core.client.scala.transform.stages.{CleanReferencesStage, DeclarationsRemovalStage, TransformationStep}
import amf.plugins.document.vocabularies.resolution.stages.DialectInstanceReferencesResolutionStage

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
