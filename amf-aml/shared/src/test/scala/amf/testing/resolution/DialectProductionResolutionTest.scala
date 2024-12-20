package amf.testing.resolution

import amf.aml.client.scala.AMLConfiguration
import amf.aml.internal.transform.pipelines.DefaultAMLTransformationPipeline
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.Syntax
import amf.testing.common.cycling.FunSuiteCycleTests
import amf.testing.common.utils.DialectInstanceTester

class DialectProductionResolutionTest extends FunSuiteCycleTests with DialectInstanceTester {

  override def transform(unit: BaseUnit, config: CycleConfig, amlConfig: AMLConfiguration): BaseUnit =
    amlConfig.baseUnitClient().transform(unit, DefaultAMLTransformationPipeline.name).baseUnit

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/production/"

  // Order is not predictable
  ignore("Can parse asyncapi overlay instances") {
    cycleWithDialect(
      "dialect6.yaml",
      "patch6.yaml",
      "patch6.resolved.yaml",
      syntax = Some(Syntax.Yaml),
      basePath + "asyncapi/"
    )
  }

}
