package amf.plugins.document.vocabularies.resolution.pipelines

import amf.client.remod.amfcore.resolution.PipelineName
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.remote.Aml
import amf.core.resolution.pipelines.{TransformationPipeline, TransformationPipelineRunner}
import amf.core.resolution.stages.TransformationStep
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, DialectInstancePatch}

class DefaultAMLTransformationPipeline(override val name: String) extends TransformationPipeline {
  override def steps: Seq[TransformationStep] = Seq(RedirectResolutionByModel)
}

private object RedirectResolutionByModel extends TransformationStep {
  override def transform[T <: BaseUnit](model: T, errorHandler: ErrorHandler): T = {
    val runner = TransformationPipelineRunner(errorHandler)
    model match {
      case _: DialectInstancePatch => runner.run(model, DialectInstancePatchTransformationPipeline())
      case _: Dialect              => runner.run(model, DialectTransformationPipeline())
      case _: DialectInstance      => runner.run(model, DialectInstanceTransformationPipeline())
      case _                       => model
    }
  }
}

object DefaultAMLTransformationPipeline {
  val name: String                              = PipelineName.from(Aml.name, TransformationPipeline.DEFAULT_PIPELINE)
  def apply(): DefaultAMLTransformationPipeline = new DefaultAMLTransformationPipeline(name)
  private[amf] def editing()                    = new DefaultAMLTransformationPipeline(name)
}

object AMLEditingPipeline {
  val name: String                              = PipelineName.from(Aml.name, TransformationPipeline.EDITING_PIPELINE)
  def apply(): DefaultAMLTransformationPipeline = DefaultAMLTransformationPipeline.editing()
}
