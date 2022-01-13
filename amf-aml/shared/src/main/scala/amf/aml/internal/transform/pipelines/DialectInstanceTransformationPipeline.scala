package amf.aml.internal.transform.pipelines

import amf.core.internal.transform.stages.{CleanReferencesStage, DeclarationsRemovalStage, SourceInformationStage}
import amf.aml.internal.transform.steps.{DialectInstanceReferencesResolutionStage, SemanticExtensionFlatteningStage}
import amf.core.client.scala.transform.{TransformationPipeline, TransformationStep}

class DialectInstanceTransformationPipeline private (override val name: String) extends TransformationPipeline() {

  override def steps: Seq[TransformationStep] =
    Seq(
        new DialectInstanceReferencesResolutionStage(),
        new CleanReferencesStage(),
        new DeclarationsRemovalStage(),
        new SemanticExtensionFlatteningStage,
        SourceInformationStage
    )

}

object DialectInstanceTransformationPipeline {
  val name: String = "DialectInstanceTransformationPipeline"
  def apply()      = new DialectInstanceTransformationPipeline(name)
}
