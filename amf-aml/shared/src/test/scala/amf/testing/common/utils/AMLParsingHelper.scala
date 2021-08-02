package amf.testing.common.utils

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}
import amf.core.internal.remote.Platform

import scala.concurrent.{ExecutionContext, Future}

trait AMLParsingHelper {

  final def parse(uri: String, platform: Platform, configuration: AMLConfiguration)(
      implicit ec: ExecutionContext): Future[BaseUnit] =
    new AMFCompiler(new CompilerContextBuilder(uri, platform, configuration.compilerConfiguration).build())
      .build()

  final def parse(uri: String, platform: Platform, vendor: Option[String], configuration: AMLConfiguration)(
      implicit ec: ExecutionContext): Future[BaseUnit] =
    new AMFCompiler(new CompilerContextBuilder(uri, platform, configuration.compilerConfiguration).build())
      .build()

}
