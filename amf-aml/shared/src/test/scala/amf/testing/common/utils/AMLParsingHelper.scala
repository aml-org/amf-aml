package amf.testing.common.utils

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.{Hint, Platform}
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}

import scala.concurrent.{ExecutionContext, Future}

trait AMLParsingHelper {

  final def parse(uri: String, platform: Platform, hint: Hint, configuration: AMLConfiguration)(
      implicit ec: ExecutionContext): Future[BaseUnit] =
    new AMFCompiler(new CompilerContextBuilder(uri, platform, configuration.parseConfiguration).build(),
                    Some(hint.vendor.mediaType))
      .build()

  final def parse(uri: String, platform: Platform, vendor: Option[String], configuration: AMLConfiguration)(
      implicit ec: ExecutionContext): Future[BaseUnit] =
    new AMFCompiler(new CompilerContextBuilder(uri, platform, configuration.parseConfiguration).build(), vendor)
      .build()

}
