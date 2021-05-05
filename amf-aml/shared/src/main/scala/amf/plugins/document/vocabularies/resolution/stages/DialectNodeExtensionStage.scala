package amf.plugins.document.vocabularies.resolution.stages

import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.resolution.stages.TransformationStep
import amf.plugins.document.vocabularies.model.domain.NodeMapping
import amf.utils.internal.AmlExtensionSyntax._

class DialectNodeExtensionStage() extends TransformationStep() {

  override def apply[T <: BaseUnit](model: T, errorHandler: ErrorHandler): T = {
    model match {
      case declarationModel: DeclaresModel =>
        declarationModel.declares.foreach {
          case nodeMapping: NodeMapping => nodeMapping.resolver.resolveExtension
          case _                        => // ignore
        }
      case _ => // ignore
    }
    model
  }

}
