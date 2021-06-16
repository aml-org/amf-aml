package amf.testing.common.cycling

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.io.FileAssertionTest
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.Syntax.Syntax
import amf.core.internal.remote.{Hint, Vendor}
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}
import amf.testing.common.utils.AMFRenderer

import scala.concurrent.{ExecutionContext, Future}

trait BuildCycleTestCommon extends FileAssertionTest {

  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  def basePath: String

  case class CycleConfig(source: String,
                         golden: String,
                         hint: Hint,
                         target: Vendor,
                         directory: String = basePath,
                         syntax: Option[Syntax] = None,
                         pipeline: Option[String] = None,
                         transformWith: Option[Vendor] = None) {
    val sourcePath: String = directory + source
    val goldenPath: String = directory + golden
  }

  /** Method to parse unit. Override if necessary. */
  def build(config: CycleConfig, amlConfig: AMLConfiguration): Future[BaseUnit] = {

    val environment =
      amlConfig.withParsingOptions(amlConfig.options.parsingOptions.withBaseUnitUrl("file://" + config.goldenPath))

    val context =
      new CompilerContextBuilder(s"file://${config.sourcePath}", platform, environment.parseConfiguration)
        .build()

    val maybeSyntax = config.syntax.map(_.toString)
    val maybeVendor = Some(config.hint.vendor.mediaType)
    new AMFCompiler(context, mediaType = maybeVendor).build()
  }

  /** Method to render parsed unit. Override if necessary. */
  def render(unit: BaseUnit, config: CycleConfig, graphConfig: AMFGraphConfiguration): String = {
    val target = config.target
    new AMFRenderer(unit, target, graphConfig, config.syntax).renderToString
  }
}
