package amf.plugins.document.vocabularies.resolution.pipelines

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.transform.pipelines.TransformationPipeline
import amf.core.client.scala.transform.stages.TransformationStep
import amf.plugins.document.vocabularies.resolution.stages.{
  DialectNodeExtensionStage,
  DialectReferencesResolutionStage
}

class DialectTransformationPipeline private (override val name: String) extends TransformationPipeline() {

  override def steps: Seq[TransformationStep] =
    Seq(new DialectReferencesResolutionStage(), new DialectNodeExtensionStage())

}

object DialectTransformationPipeline {
  val name: String = DialectTransformationPipeline.getClass.getSimpleName
  def apply()      = new DialectTransformationPipeline(name)
}
