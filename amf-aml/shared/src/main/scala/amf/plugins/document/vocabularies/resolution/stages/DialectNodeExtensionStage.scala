package amf.plugins.document.vocabularies.resolution.stages

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.client.scala.transform.stages.TransformationStep
import amf.plugins.document.vocabularies.model.domain.NodeMapping
import amf.utils.internal.AmlExtensionSyntax._

class DialectNodeExtensionStage() extends TransformationStep() {

  override def transform(model: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit = {
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
