package amf.testing.resolution

import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.remote.Vendor.AML
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.services.RuntimeResolver
import amf.plugins.document.vocabularies.AMLPlugin
import amf.testing.common.cycling.FunSuiteCycleTests

abstract class DialectResolutionCycleTests extends FunSuiteCycleTests {
  override def transform(unit: BaseUnit, config: CycleConfig): BaseUnit =
    RuntimeResolver.resolve(AML.name, unit, ResolutionPipeline.DEFAULT_PIPELINE, UnhandledErrorHandler)
}
