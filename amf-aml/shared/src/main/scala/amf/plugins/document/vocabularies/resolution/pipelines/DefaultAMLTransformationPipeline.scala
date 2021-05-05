package amf.plugins.document.vocabularies.resolution.pipelines

import amf.client.remod.amfcore.resolution.PipelineName
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.remote.Aml
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.TransformationStep
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, DialectInstancePatch}

class DefaultAMLTransformationPipeline(override val name: String) extends TransformationPipeline {
  override def steps: Seq[TransformationStep] = Seq(RedirectResolutionByModel)
}

private object RedirectResolutionByModel extends TransformationStep {
  override def apply[T <: BaseUnit](model: T, errorHandler: ErrorHandler): T =
    model match {
      case _: DialectInstancePatch => DialectInstancePatchResolutionPipeline().transform(model, errorHandler)
      case _: Dialect              => DialectResolutionPipeline().transform(model, errorHandler)
      case _: DialectInstance      => DialectInstanceResolutionPipeline().transform(model, errorHandler)
      case _                       => model
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
