package amf.testing.common.cycling

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.errorhandling.{AMFErrorHandler, UnhandledErrorHandler}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.Syntax.Syntax
import amf.core.internal.remote.{Hint, Vendor}
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

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, golden: String, hint: Hint, directory: String, eh: AMFErrorHandler): Future[Assertion] =
    cycle(source,
          golden,
          hint,
          hint.vendor,
          directory,
          amlConfig = AMLConfiguration.predefined().withErrorHandlerProvider(() => eh))

  /** Compile source with specified hint. Render to temporary file and assert against golden. */
  final def cycle(source: String,
                  golden: String,
                  hint: Hint,
                  target: Vendor,
                  directory: String = basePath,
                  amlConfig: AMLConfiguration =
                    AMLConfiguration.predefined().withErrorHandlerProvider(() => UnhandledErrorHandler),
                  syntax: Option[Syntax] = None,
                  pipeline: Option[String] = None,
                  transformWith: Option[Vendor] = None): Future[Assertion] = {

    val config = CycleConfig(source, golden, hint, target, directory, syntax, pipeline, transformWith)

    build(config, amlConfig)
      .map(transform(_, config, amlConfig))
      .map {
        render(_, config, amlConfig)
      }
      .flatMap(writeTemporaryFile(golden))
      .flatMap(assertDifferences(_, config.goldenPath))
  }

  /** Method for transforming parsed unit. Override if necessary. */
  def transform(unit: BaseUnit, config: CycleConfig, amlConfig: AMLConfiguration): BaseUnit = unit
}
