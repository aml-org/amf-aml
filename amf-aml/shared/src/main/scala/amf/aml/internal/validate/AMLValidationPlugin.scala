package amf.aml.internal.validate

import amf.aml.client.scala.model.document.{DialectInstance, DialectInstanceUnit}
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.core.client.common.{HighPriority, PluginPriority}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.validation.{AMFValidatePlugin, ValidationInfo, ValidationOptions, ValidationResult}

import scala.concurrent.{ExecutionContext, Future}

object AMLValidationPlugin {
  protected val id: String = this.getClass.getSimpleName
}

class AMLValidationPlugin() extends AMFValidatePlugin {

  override val id: String = AMLValidationPlugin.id

  override def priority: PluginPriority = HighPriority

  override def applies(info: ValidationInfo): Boolean = info.baseUnit.isInstanceOf[DialectInstanceUnit]

  override def validate(unit: BaseUnit, options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationResult] = {
    new AMLValidator().validate(unit, options)
  }

}
