package amf.testing.common.utils

import amf.client.environment.AMLConfiguration
import amf.client.parse.DefaultErrorHandler
import amf.client.remod.ParseConfiguration
import amf.core.remote.Aml
import amf.core.unsafe.PlatformBuilder.platform
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.Dialect

import scala.concurrent.{ExecutionContext, Future}

trait DialectRegistrationHelper {
  protected def withDialect[T](uri: String)(fn: (Dialect, AMLConfiguration) => Future[T])(
      implicit ec: ExecutionContext): Future[T] = {
    val eh            = DefaultErrorHandler()
    val configuration = AMLConfiguration.forEH(eh)
    val context       = new CompilerContextBuilder(uri, platform, ParseConfiguration(configuration)).build()
    for {
      baseUnit      <- new AMFCompiler(context, Some(Aml.mediaType)).build()
      dialect       <- Future.successful { baseUnit.asInstanceOf[Dialect] }
      dialectConfig <- Future.successful(configuration.withDialect(dialect))
      result        <- fn(dialect, dialectConfig)
      _             <- Future.successful(AMLPlugin.registry.reset()) // TODO ARM remove?
    } yield {
      result
    }
  }
}
