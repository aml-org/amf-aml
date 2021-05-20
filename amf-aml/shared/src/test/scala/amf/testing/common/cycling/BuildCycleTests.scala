package amf.testing.common.cycling

import amf.client.parse.DefaultParserErrorHandler
import amf.core.emitter.RenderOptions
import amf.core.model.document.BaseUnit
import amf.core.parser.errorhandler.ParserErrorHandler
import amf.core.remote.Syntax.Syntax
import amf.core.remote.{Hint, Vendor}
import org.scalatest.Assertion

import scala.concurrent.Future

trait BuildCycleTests extends BuildCycleTestCommon {

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, hint: Hint, syntax: Option[Syntax]): Future[Assertion] =
    cycle(source, hint, basePath, syntax)

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, hint: Hint): Future[Assertion] = cycle(source, hint, basePath, None)

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, hint: Hint, directory: String, syntax: Option[Syntax]): Future[Assertion] =
    cycle(source, source, hint, hint.vendor, directory, syntax = syntax, eh = None)

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, hint: Hint, directory: String): Future[Assertion] =
    cycle(source, source, hint, hint.vendor, directory, eh = None)

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, golden: String, hint: Hint, directory: String, eh: ParserErrorHandler): Future[Assertion] =
    cycle(source, golden, hint, hint.vendor, directory, eh = Some(eh))

  /** Compile source with specified hint. Render to temporary file and assert against golden. */
  final def cycle(source: String,
                  golden: String,
                  hint: Hint,
                  target: Vendor,
                  directory: String = basePath,
                  renderOptions: Option[RenderOptions] = None,
                  useAmfJsonldSerialization: Boolean = true,
                  syntax: Option[Syntax] = None,
                  pipeline: Option[String] = None,
                  transformWith: Option[Vendor] = None,
                  eh: Option[ParserErrorHandler] = None): Future[Assertion] = {

    val config                 = CycleConfig(source, golden, hint, target, directory, syntax, pipeline, transformWith)
    val amfJsonLdSerialization = renderOptions.map(_.isAmfJsonLdSerilization).getOrElse(useAmfJsonldSerialization)

    build(config, eh.orElse(Some(DefaultParserErrorHandler.withRun())), amfJsonLdSerialization)
      .map(transform(_, config))
      .flatMap {
        renderOptions match {
          case Some(options) => render(_, config, options)
          case None          => render(_, config, useAmfJsonldSerialization)
        }
      }
      .flatMap(writeTemporaryFile(golden))
      .flatMap(assertDifferences(_, config.goldenPath))
  }

  /** Method for transforming parsed unit. Override if necessary. */
  def transform(unit: BaseUnit, config: CycleConfig): BaseUnit = unit
}
