package amf.testing.common.cycling

import amf.client.environment.AMLConfiguration
import amf.core.emitter.RenderOptions
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.document.BaseUnit
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
    cycle(source, source, hint, hint.vendor, directory, syntax = syntax)

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, hint: Hint, directory: String): Future[Assertion] =
    cycle(source, source, hint, hint.vendor, directory)

  /** Compile source with specified hint. Render to temporary file and assert against golden. */
  final def cycle(source: String,
                  golden: String,
                  hint: Hint,
                  target: Vendor,
                  directory: String = basePath,
                  amlConfig: AMLConfiguration =
                    AMLConfiguration.predefined().withErrorHandlerProvider(() => UnhandledErrorHandler),
                  renderOptions: Option[RenderOptions] = None,
                  useAmfJsonldSerialization: Boolean = true,
                  syntax: Option[Syntax] = None,
                  pipeline: Option[String] = None,
                  transformWith: Option[Vendor] = None): Future[Assertion] = {

    val config                 = CycleConfig(source, golden, hint, target, directory, syntax, pipeline, transformWith)
    val amfJsonLdSerialization = renderOptions.map(_.isAmfJsonLdSerilization).getOrElse(useAmfJsonldSerialization)

    build(config, amlConfig, amfJsonLdSerialization)
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
