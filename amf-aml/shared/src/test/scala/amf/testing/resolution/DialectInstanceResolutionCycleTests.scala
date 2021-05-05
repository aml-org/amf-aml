package amf.testing.resolution

import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.remote.Vendor.AML
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.services.RuntimeResolver
import amf.plugins.document.vocabularies.AMLPlugin
import amf.testing.common.utils.DialectTests

abstract class DialectInstanceResolutionCycleTests extends DialectTests {
  override def transform(unit: BaseUnit): BaseUnit =
    RuntimeResolver.resolve(AML.name, unit, TransformationPipeline.DEFAULT_PIPELINE, UnhandledErrorHandler)
}
