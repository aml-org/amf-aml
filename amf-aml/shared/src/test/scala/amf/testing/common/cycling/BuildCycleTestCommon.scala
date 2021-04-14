package amf.testing.common.cycling

import amf.client.remod.amfcore.config.ParsingOptionsConverter
import amf.core.client.ParsingOptions
import amf.core.emitter.RenderOptions
import amf.core.io.FileAssertionTest
import amf.core.model.document.BaseUnit
import amf.core.parser.errorhandler.{ParserErrorHandler, UnhandledParserErrorHandler}
import amf.core.registries.AMFPluginsRegistry
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
  def build(config: CycleConfig,
            eh: Option[ParserErrorHandler],
            useAmfJsonldSerialisation: Boolean): Future[BaseUnit] = {

    var options =
      if (!useAmfJsonldSerialisation) ParsingOptions().withoutAmfJsonLdSerialization
      else ParsingOptions().withAmfJsonLdSerialization

    options = options.withBaseUnitUrl("file://" + config.goldenPath)

    val environment =
      AMFPluginsRegistry.obtainStaticConfig().withParsingOptions(ParsingOptionsConverter.fromLegacy(options))
    val context =
      new CompilerContextBuilder(s"file://${config.sourcePath}", platform, eh.getOrElse(UnhandledParserErrorHandler))
        .withBaseEnvironment(environment)
        .build()

    val maybeSyntax = config.syntax.map(_.toString)
    val maybeVendor = Some(config.hint.vendor.name)
    new AMFCompiler(context, mediaType = maybeSyntax, vendor = maybeVendor).build()
  }

  /** Method to render parsed unit. Override if necessary. */
  def render(unit: BaseUnit, config: CycleConfig, useAmfJsonldSerialization: Boolean): Future[String] = {
    val target  = config.target
    var options = RenderOptions().withSourceMaps.withPrettyPrint
    options =
      if (!useAmfJsonldSerialization) options.withoutAmfJsonLdSerialization else options.withAmfJsonLdSerialization
    new AMFRenderer(unit, target, options, config.syntax).renderToString
  }

  /** Method to render parsed unit. Override if necessary. */
  def render(unit: BaseUnit, config: CycleConfig, options: RenderOptions): Future[String] = {
    val target = config.target
    new AMFRenderer(unit, target, options, config.syntax).renderToString
  }
}
