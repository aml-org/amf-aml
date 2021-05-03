package amf.plugins.document.vocabularies.resolution.pipelines

import amf.client.remod.amfcore.resolution.PipelineName
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.remote.Aml
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.resolution.stages.ResolutionStage
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, DialectInstancePatch}

class DefaultAMLTransformationPipeline(override val name: String) extends ResolutionPipeline {
  override def steps(implicit errorHandler: ErrorHandler): Seq[ResolutionStage] = Seq(new RedirectResolutionByModel())
}

private class RedirectResolutionByModel(override implicit val errorHandler: ErrorHandler) extends ResolutionStage {
  override def resolve[T <: BaseUnit](model: T): T =
    model match {
      case _: DialectInstancePatch => DialectInstancePatchResolutionPipeline().transform(model, errorHandler)
      case _: Dialect              => DialectResolutionPipeline().transform(model, errorHandler)
      case _: DialectInstance      => DialectInstanceResolutionPipeline().transform(model, errorHandler)
      case _                       => model
    }
}

object DefaultAMLTransformationPipeline {
  val name: String                              = PipelineName.from(Aml.name, ResolutionPipeline.DEFAULT_PIPELINE)
  def apply(): DefaultAMLTransformationPipeline = new DefaultAMLTransformationPipeline(name)
  private[amf] def editing()                    = new DefaultAMLTransformationPipeline(name)
}

object AMLEditingPipeline {
  val name: String                              = PipelineName.from(Aml.name, ResolutionPipeline.EDITING_PIPELINE)
  def apply(): DefaultAMLTransformationPipeline = DefaultAMLTransformationPipeline.editing()
}
