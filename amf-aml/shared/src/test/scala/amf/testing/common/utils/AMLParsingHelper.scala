package amf.testing.common.utils

import amf.client.environment.AMLConfiguration
import amf.client.remod.ParseConfiguration
import amf.core.model.document.BaseUnit
import amf.core.remote.{Hint, Platform}
import amf.core.{AMFCompiler, CompilerContextBuilder}

import scala.concurrent.{ExecutionContext, Future}

trait AMLParsingHelper {

  final def parse(uri: String, platform: Platform, hint: Hint, configuration: AMLConfiguration)(
      implicit ec: ExecutionContext): Future[BaseUnit] =
    new AMFCompiler(new CompilerContextBuilder(platform, new ParseConfiguration(configuration, uri)).build(),
                    Some(hint.vendor.mediaType))
      .build()

  final def parse(uri: String, platform: Platform, vendor: Option[String], configuration: AMLConfiguration)(
      implicit ec: ExecutionContext): Future[BaseUnit] =
    new AMFCompiler(new CompilerContextBuilder(platform, new ParseConfiguration(configuration, uri)).build(), vendor)
      .build()

}
