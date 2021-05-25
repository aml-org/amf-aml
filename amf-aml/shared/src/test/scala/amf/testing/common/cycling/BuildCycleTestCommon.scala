package amf.testing.common.cycling

import amf.client.environment.AMLConfiguration
import amf.client.remod.{AMFGraphConfiguration, ParseConfiguration}
import amf.client.remod.amfcore.config.{ParsingOptionsConverter, RenderOptions}
import amf.core.client.ParsingOptions
import amf.core.io.FileAssertionTest
import amf.core.model.document.BaseUnit
import amf.core.remote.Syntax.Syntax
import amf.core.remote.{Hint, Vendor}
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.testing.common.utils.AMFRenderer

import scala.concurrent.{ExecutionContext, Future}

trait BuildCycleTestCommon extends FileAssertionTest {

  protected implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  def basePath: String

  case class CycleConfig(source: String,
                         golden: String,
                         hint: Hint,
                         target: Vendor,
                         directory: String,
                         syntax: Option[Syntax],
                         pipeline: Option[String],
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
  def render(unit: BaseUnit, config: CycleConfig, graphConfig: AMFGraphConfiguration): Future[String] = {
    val target = config.target
    new AMFRenderer(unit, target, graphConfig, config.syntax).renderToString
  }
}
