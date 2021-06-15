package amf.testing.resolution

import amf.client.environment.AMLConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.plugins.document.vocabularies.resolution.pipelines.DefaultAMLTransformationPipeline
import amf.testing.common.utils.DialectTests

abstract class DialectInstanceResolutionCycleTests extends DialectTests {
  override def transform(unit: BaseUnit, amlConfig: AMLConfiguration): BaseUnit =
    amlConfig.createClient().transform(unit, DefaultAMLTransformationPipeline.name).bu
}
