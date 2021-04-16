package amf.dialects

import amf.ProfileName
import amf.client.parse.DefaultParserErrorHandler
import amf.core.services.RuntimeValidator
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.AMFValidationReport
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.features.validation.custom.AMFValidatorPlugin
import org.scalatest.AsyncFunSuite

import scala.concurrent.Future

trait DialectInstanceValidation extends AsyncFunSuite with PlatformSecrets {

  def basePath: String

  protected def validation(dialect: String, instance: String, path: String = basePath): Future[AMFValidationReport] = {
    val dialectContext  = compilerContext(s"$path/$dialect")
    val instanceContext = compilerContext(s"$path/$instance")

    for {
      dialect <- {
        new AMFCompiler(
            dialectContext,
            Some("application/yaml"),
            None
        ).build()
      }
      instance <- {
        new AMFCompiler(
            instanceContext,
            Some("application/yaml"),
            None
        ).build()
      }
      report <- RuntimeValidator(instance, ProfileName(dialect.asInstanceOf[Dialect].nameAndVersion()))
    } yield {
      report
    }
  }

  protected def validationWithCustomProfile(dialect: String,
                                            instance: String,
                                            profile: ProfileName,
                                            name: String,
                                            directory: String = basePath): Future[AMFValidationReport] = {
    val dialectContext = compilerContext(s"$directory/$dialect")
    for {
      dialect <- {
        new AMFCompiler(
            dialectContext,
            Some("application/yaml"),
            None
        ).build()
      }
      _ <- Future.successful(AMLPlugin.registry.register(dialect.asInstanceOf[Dialect]))
      profile <- {
        AMFValidatorPlugin.loadValidationProfile(s"$directory/${profile.profile}",
                                                 errorHandler = dialectContext.parserContext.eh)
      }
      instance <- {
        val instanceContext = compilerContext(s"$directory/$instance")
        new AMFCompiler(
            instanceContext,
            Some("application/yaml"),
            None
        ).build()
      }
      report <- {
        RuntimeValidator(
            instance,
            ProfileName(name)
        )
      }
      _ <- Future.successful(AMLPlugin.registry.unregisterDialect(dialect.id))
    } yield {
      report
    }
  }

  private def compilerContext(url: String) =
    new CompilerContextBuilder(url, platform, eh = DefaultParserErrorHandler.withRun()).build()

}
