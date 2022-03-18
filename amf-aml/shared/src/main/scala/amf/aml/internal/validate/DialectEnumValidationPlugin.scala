package amf.aml.internal.validate

import amf.aml.client.scala.model.document.Dialect
import amf.core.client.common.{HighPriority, PluginPriority}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.plugins.validation.{AMFValidatePlugin, ValidationInfo, ValidationOptions, ValidationResult}
import amf.core.internal.validation.ShaclReportAdaptation

import scala.concurrent.{ExecutionContext, Future}

object DialectEnumValidationPlugin extends AMFValidatePlugin {
  override val id: String = "dialect-enum-validation-plugin"

  override def priority: PluginPriority = HighPriority

  override def applies(element: ValidationInfo): Boolean = element.baseUnit.isInstanceOf[Dialect]

  override def validate(unit: BaseUnit, options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationResult] = {
    unit match {
      case dialect: Dialect =>
        val validator = DialectEnumValidator()
        val report    = validator.validate(dialect)
        Future.successful(ValidationResult(dialect, report))
    }
  }
}
