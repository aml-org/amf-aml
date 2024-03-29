package amf.aml.internal.transform.pipelines

import amf.core.client.common.transform._
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.aml.client.scala.model.document.{Dialect, DialectInstance, DialectInstancePatch}
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.transform.{TransformationPipeline, TransformationPipelineRunner, TransformationStep}

class DefaultAMLTransformationPipeline(override val name: String) extends TransformationPipeline {
  override def steps: Seq[TransformationStep] = Seq(RedirectResolutionByModel)
}

private object RedirectResolutionByModel extends TransformationStep {
  override def transform(
      model: BaseUnit,
      errorHandler: AMFErrorHandler,
      configuration: AMFGraphConfiguration
  ): BaseUnit = {
    val runner = TransformationPipelineRunner(errorHandler, configuration)
    model match {
      case _: DialectInstancePatch => runner.run(model, DialectInstancePatchTransformationPipeline())
      case _: Dialect              => runner.run(model, DialectTransformationPipeline())
      case _: DialectInstance      => runner.run(model, DialectInstanceTransformationPipeline())
      case _                       => model
    }
  }
}

object DefaultAMLTransformationPipeline {
  val name: String                              = PipelineId.Default
  def apply(): DefaultAMLTransformationPipeline = new DefaultAMLTransformationPipeline(name)
  private[amf] def editing()                    = new DefaultAMLTransformationPipeline(name)
}

object AMLEditingPipeline {
  val name: String                              = PipelineId.Editing
  def apply(): DefaultAMLTransformationPipeline = DefaultAMLTransformationPipeline.editing()
}
