package amf.aml.internal.transform.pipelines

import amf.aml.internal.transform.steps.{
  DialectCombiningMappingStage,
  DialectNodeExtensionStage,
  DialectReferencesResolutionStage
}
import amf.core.client.scala.transform.{TransformationPipeline, TransformationStep}
import amf.core.internal.transform.stages.SourceInformationStage

class DialectTransformationPipeline private (override val name: String) extends TransformationPipeline() {

  override def steps: Seq[TransformationStep] =
    Seq(
        new DialectCombiningMappingStage(),
        new DialectReferencesResolutionStage(),
        new DialectNodeExtensionStage(),
        SourceInformationStage
    )

}

object DialectTransformationPipeline {
  val name: String = DialectTransformationPipeline.getClass.getSimpleName
  def apply()      = new DialectTransformationPipeline(name)
}
