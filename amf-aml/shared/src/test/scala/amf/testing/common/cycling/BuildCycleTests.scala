package amf.testing.common.cycling

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.errorhandling.{AMFErrorHandler, UnhandledErrorHandler}
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.Spec
import amf.core.internal.remote.Syntax.Syntax
import org.scalatest.Assertion

import scala.concurrent.Future

trait BuildCycleTests extends BuildCycleTestCommon {

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, syntax: Option[Syntax]): Future[Assertion] =
    cycle(source, basePath, syntax)

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String): Future[Assertion] = cycle(source, basePath, None)

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, directory: String, syntax: Option[Syntax]): Future[Assertion] =
    cycle(source, source, syntax = syntax, directory)

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, directory: String): Future[Assertion] =
    cycle(source, source, None, directory)

  /** Compile source with specified hint. Dump to target and assert against same source file. */
  def cycle(source: String, golden: String, directory: String, eh: AMFErrorHandler): Future[Assertion] =
    cycle(source,
          golden,
          None,
          directory,
          amlConfig = AMLConfiguration.predefined().withErrorHandlerProvider(() => eh))

  /** Compile source with specified hint. Render to temporary file and assert against golden. */
  final def cycle(source: String,
                  golden: String,
                  syntax: Option[Syntax],
                  directory: String = basePath,
                  amlConfig: AMLConfiguration =
                    AMLConfiguration.predefined().withErrorHandlerProvider(() => UnhandledErrorHandler),
                  pipeline: Option[String] = None,
                  transformWith: Option[Spec] = None): Future[Assertion] = {

    val config = CycleConfig(source, golden, directory, syntax, pipeline, transformWith)

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
