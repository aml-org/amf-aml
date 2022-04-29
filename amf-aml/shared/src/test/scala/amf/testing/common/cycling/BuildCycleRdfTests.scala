package amf.testing.common.cycling

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.Spec
import amf.core.internal.remote.Syntax.Syntax
import org.scalatest.Assertion
import amf.rdf.client.scala.{RdfModel, RdfUnitConverter}

import scala.concurrent.Future

trait BuildCycleRdfTests extends BuildCycleTestCommon {

  def cycleFullRdf(
      source: String,
      golden: String,
      directory: String = basePath,
      amlConfig: AMLConfiguration = AMLConfiguration.predefined().withErrorHandlerProvider(() => UnhandledErrorHandler),
      syntax: Option[Syntax] = None,
      pipeline: Option[String] = None
  ): Future[Assertion] = {

    val config = CycleConfig(source, golden, directory, syntax, pipeline, None)

    build(config, amlConfig)
      .map(transformThroughRdf(_, config, amlConfig))
      .map {
        render(_, config, amlConfig)
      }
      .flatMap(writeTemporaryFile(golden))
      .flatMap(assertDifferences(_, config.goldenPath))
  }

  /** Compile source with specified hint. Render to temporary file and assert against golden. */
  def cycleRdf(
      source: String,
      golden: String,
      amlConfig: AMLConfiguration,
      directory: String = basePath,
      syntax: Option[Syntax] = None,
      pipeline: Option[String] = None,
      transformWith: Option[Spec] = None
  ): Future[Assertion] = {

    val config = CycleConfig(source, golden, directory, syntax, pipeline, transformWith)

    build(config, amlConfig)
      .map(transformRdf(_, config))
      .flatMap(renderRdf(_, config))
      .flatMap(writeTemporaryFile(golden))
      .flatMap(assertDifferences(_, config.goldenPath))
  }

  /** Method for transforming parsed unit. Override if necessary. */
  def transformRdf(unit: BaseUnit, config: CycleConfig): RdfModel = {
    RdfUnitConverter.toNativeRdfModel(unit)
  }

  /** Method for transforming parsed unit. Override if necessary. */
  def transformThroughRdf(unit: BaseUnit, config: CycleConfig, amfConfig: AMFGraphConfiguration): BaseUnit = {
    val rdfModel = RdfUnitConverter.toNativeRdfModel(unit, RenderOptions().withSourceMaps)
    RdfUnitConverter.fromNativeRdfModel(unit.id, rdfModel, amfConfig)
  }

  /** Method to render parsed unit. Override if necessary. */
  def renderRdf(unit: RdfModel, config: CycleConfig): Future[String] = {
    Future {
      unit.toN3().split("\n").sorted.mkString("\n")
    }
  }
}
