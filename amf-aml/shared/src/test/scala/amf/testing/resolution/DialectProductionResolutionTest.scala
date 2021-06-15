package amf.testing.resolution

import amf.client.environment.AMLConfiguration
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.remote.{Aml, VocabularyYamlHint}
import amf.plugins.document.vocabularies.resolution.pipelines.DefaultAMLTransformationPipeline
import amf.testing.common.cycling.FunSuiteCycleTests
import amf.testing.common.utils.DialectInstanceTester

import scala.concurrent.ExecutionContext

class DialectProductionResolutionTest extends FunSuiteCycleTests with DialectInstanceTester {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  override def transform(unit: BaseUnit, config: CycleConfig, amlConfig: AMLConfiguration): BaseUnit =
    amlConfig.createClient().transform(unit, DefaultAMLTransformationPipeline.name).bu

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/production/"

  // Order is not predictable
  ignore("Can parse asyncapi overlay instances") {
    cycleWithDialect("dialect6.yaml",
                     "patch6.yaml",
                     "patch6.resolved.yaml",
                     VocabularyYamlHint,
                     Aml,
                     basePath + "asyncapi/")
  }

}
