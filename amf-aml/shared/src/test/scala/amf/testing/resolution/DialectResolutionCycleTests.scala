package amf.testing.resolution

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.aml.internal.transform.pipelines.DefaultAMLTransformationPipeline
import amf.testing.common.cycling.FunSuiteCycleTests

abstract class DialectResolutionCycleTests extends FunSuiteCycleTests {
  override def transform(unit: BaseUnit, config: CycleConfig, amlConfig: AMLConfiguration): BaseUnit =
    amlConfig.baseUnitClient().transform(unit, DefaultAMLTransformationPipeline.name).bu
}
