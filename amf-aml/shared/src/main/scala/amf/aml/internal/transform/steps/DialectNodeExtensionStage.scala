package amf.aml.internal.transform.steps

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.aml.client.scala.model.domain.NodeMapping
import amf.aml.internal.utils.AmlExtensionSyntax._
import amf.core.client.scala.transform.TransformationStep

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
