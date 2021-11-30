package amf.testing.common.utils

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}
import amf.core.internal.remote.Platform

import scala.concurrent.{ExecutionContext, Future}

trait AMLParsingHelper {

  final def parse(uri: String, configuration: AMLConfiguration)(implicit ec: ExecutionContext): Future[BaseUnit] =
    configuration.baseUnitClient().parse(uri).map(_.baseUnit)
}
