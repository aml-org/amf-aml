package amf.testing.common.cycling

import amf.client.environment.AMLConfiguration
import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.config.RenderOptions
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.rdf.RdfModel
import amf.core.remote.Syntax.Syntax
import amf.core.remote.{Amf, Hint, Vendor}
import org.scalatest.Assertion

import scala.concurrent.Future

trait BuildCycleRdfTests extends BuildCycleTestCommon {

  def cycleFullRdf(source: String,
                   golden: String,
                   hint: Hint,
                   target: Vendor = Amf,
                   directory: String = basePath,
                   amlConfig: AMLConfiguration =
                     AMLConfiguration.predefined().withErrorHandlerProvider(() => UnhandledErrorHandler),
                   syntax: Option[Syntax] = None,
                   pipeline: Option[String] = None): Future[Assertion] = {

    val config = CycleConfig(source, golden, hint, target, directory, syntax, pipeline, None)

    build(config, amlConfig)
      .map(transformThroughRdf(_, config, amlConfig))
      .flatMap {
        render(_, config, amlConfig)
      }
      .flatMap(writeTemporaryFile(golden))
      .flatMap(assertDifferences(_, config.goldenPath))
  }

  /** Compile source with specified hint. Render to temporary file and assert against golden. */
  def cycleRdf(source: String,
               golden: String,
               hint: Hint,
               target: Vendor = Amf,
               amlConfig: AMLConfiguration,
               directory: String = basePath,
               syntax: Option[Syntax] = None,
               pipeline: Option[String] = None,
               transformWith: Option[Vendor] = None): Future[Assertion] = {

    val config = CycleConfig(source, golden, hint, target, directory, syntax, pipeline, transformWith)

    build(config, amlConfig)
      .map(transformRdf(_, config))
      .flatMap(renderRdf(_, config))
      .flatMap(writeTemporaryFile(golden))
      .flatMap(assertDifferences(_, config.goldenPath))
  }

  /** Method for transforming parsed unit. Override if necessary. */
  def transformRdf(unit: BaseUnit, config: CycleConfig): RdfModel = {
    unit.toNativeRdfModel()
  }

  /** Method for transforming parsed unit. Override if necessary. */
  def transformThroughRdf(unit: BaseUnit, config: CycleConfig, amfConfig: AMFGraphConfiguration): BaseUnit = {
    val rdfModel = unit.toNativeRdfModel(RenderOptions().withSourceMaps)
    BaseUnit.fromNativeRdfModel(unit.id, rdfModel, amfConfig)
  }

  /** Method to render parsed unit. Override if necessary. */
  def renderRdf(unit: RdfModel, config: CycleConfig): Future[String] = {
    Future {
      unit.toN3().split("\n").sorted.mkString("\n")
    }
  }
}
