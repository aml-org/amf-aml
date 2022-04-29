package amf.testing.common.cycling

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}
import amf.core.internal.remote.Spec
import amf.core.internal.remote.Syntax.Syntax
import amf.core.io.FileAssertionTest
import amf.testing.common.utils.AMFRenderer

import scala.concurrent.{ExecutionContext, Future}

trait BuildCycleTestCommon extends FileAssertionTest {

  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  def basePath: String

  case class CycleConfig(
      source: String,
      golden: String,
      directory: String = basePath,
      syntax: Option[Syntax] = None,
      pipeline: Option[String] = None,
      transformWith: Option[Spec] = None
  ) {
    val sourcePath: String = directory + source
    val goldenPath: String = directory + golden
  }

  /** Method to parse unit. Override if necessary. */
  def build(config: CycleConfig, amlConfig: AMLConfiguration): Future[BaseUnit] = {

    val environment =
      amlConfig.withParsingOptions(amlConfig.options.parsingOptions.withBaseUnitUrl("file://" + config.goldenPath))

    val context =
      new CompilerContextBuilder(s"file://${config.sourcePath}", platform, environment.compilerConfiguration)
        .build()

    new AMFCompiler(context).build()
  }

  /** Method to render parsed unit. Override if necessary. */
  def render(unit: BaseUnit, config: CycleConfig, graphConfig: AMFGraphConfiguration): String = {
    new AMFRenderer(unit, graphConfig, config.syntax).renderToString
  }
}
