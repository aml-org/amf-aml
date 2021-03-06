package amf.plugins.document.vocabularies.resolution.stages

import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.resolution.stages.ResolutionStage
import amf.plugins.document.vocabularies.model.domain.NodeMapping
import amf.utils.internal.AmlExtensionSyntax._

class DialectNodeExtensionStage()(override implicit val errorHandler: ErrorHandler) extends ResolutionStage() {

  override def resolve[T <: BaseUnit](model: T): T = {
    model match {
      case declarationModel: DeclaresModel =>
        declarationModel.declares.foreach {
          case nodeMapping: NodeMapping => nodeMapping.resolver.resolveExtension
          case _ => // ignore
        }
      case _ => // ignore
    }
    model
  }

}
