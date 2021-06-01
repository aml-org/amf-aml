package amf.testing.resolution

import amf.client.environment.AMLConfiguration
import amf.core.model.document.BaseUnit
import amf.plugins.document.vocabularies.resolution.pipelines.DefaultAMLTransformationPipeline
import amf.testing.common.cycling.FunSuiteCycleTests

abstract class DialectResolutionCycleTests extends FunSuiteCycleTests {
  override def transform(unit: BaseUnit, config: CycleConfig, amlConfig: AMLConfiguration): BaseUnit =
    amlConfig.createClient().transform(unit, DefaultAMLTransformationPipeline.name).bu
}
