package amf.dialects

import amf.client.parse.DefaultParserErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.remote.{Hint, Platform}
import amf.core.{AMFCompiler, CompilerContextBuilder}

import scala.concurrent.{ExecutionContext, Future}

trait AMLParsingHelper {

  final def parse(uri: String, platform: Platform, hint: Hint)(implicit ec: ExecutionContext): Future[BaseUnit] =
    new AMFCompiler(new CompilerContextBuilder(uri, platform, DefaultParserErrorHandler.withRun()).build(),
                    None,
                    Some(hint.vendor.name))
      .build()

  final def parse(uri: String, platform: Platform, vendor: Option[String])(
      implicit ec: ExecutionContext): Future[BaseUnit] =
    new AMFCompiler(new CompilerContextBuilder(uri, platform, DefaultParserErrorHandler.withRun()).build(),
                    None,
                    vendor)
      .build()

}
