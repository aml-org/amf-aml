package amf.testing.resolution

import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.remote.Vendor.AML
import amf.core.remote.{Aml, VocabularyYamlHint}
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.services.RuntimeResolver
import amf.plugins.document.vocabularies.AMLPlugin
import amf.testing.common.cycling.FunSuiteCycleTests
import amf.testing.common.utils.DialectInstanceTester

import scala.concurrent.ExecutionContext

class DialectProductionResolutionTest extends FunSuiteCycleTests with DialectInstanceTester {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  override def transform(unit: BaseUnit, config: CycleConfig): BaseUnit =
    RuntimeResolver.resolve(AML.name, unit, TransformationPipeline.DEFAULT_PIPELINE, UnhandledErrorHandler)

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
