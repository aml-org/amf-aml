package amf.testing.common.utils

import amf.client.parse.DefaultParserErrorHandler
import amf.core.errorhandling.{ErrorHandler, UnhandledErrorHandler}
import amf.core.remote.Aml
import amf.core.unsafe.PlatformBuilder.platform
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.Dialect

import scala.concurrent.{ExecutionContext, Future}

trait DialectRegistrationHelper {
  protected def withDialect[T](uri: String, eh: ErrorHandler = UnhandledErrorHandler)(fn: Dialect => Future[T])(
      implicit ec: ExecutionContext): Future[T] = {
    val context = new CompilerContextBuilder(uri, platform, DefaultParserErrorHandler.withRun()).build()
    for {
      baseUnit <- new AMFCompiler(context, None, Some(Aml.name)).build()
      dialect  <- Future.successful(baseUnit.asInstanceOf[Dialect])
      _        <- Future.successful(AMLPlugin.resolve(dialect, eh))
      _        <- Future.successful(AMLPlugin.registry.register(dialect))
      result   <- fn(dialect)
      _        <- Future.successful(AMLPlugin.registry.reset())
    } yield {
      result
    }
  }
}
