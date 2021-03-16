package amf.dialects

import amf.client.environment.AMLEnvironment
import amf.client.parse.DefaultParserErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote.{Hint, Platform}
import amf.core.{AMFCompiler, CompilerContextBuilder}

import scala.concurrent.{ExecutionContext, Future}

trait DialectHelper {

  final def parseAndRegisterDialect(uri: String, platform: Platform, hint: Hint)(
      implicit ec: ExecutionContext): Future[BaseUnit] =
    for {
      r <- new AMFCompiler(new CompilerContextBuilder(uri, platform, DefaultParserErrorHandler.withRun())
        .build(AMLEnvironment.aml()),
                           None,
                           Some(hint.vendor.name))
        .build()
    } yield {
      r
    }
}
