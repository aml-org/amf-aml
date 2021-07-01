package amf.testing.resolution

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.aml.internal.transform.pipelines.DefaultAMLTransformationPipeline
import amf.testing.common.utils.DialectTests

abstract class DialectInstanceResolutionCycleTests extends DialectTests {
  override def transform(unit: BaseUnit, amlConfig: AMLConfiguration): BaseUnit =
    amlConfig.baseUnitClient().transform(unit, DefaultAMLTransformationPipeline.name).bu
}
