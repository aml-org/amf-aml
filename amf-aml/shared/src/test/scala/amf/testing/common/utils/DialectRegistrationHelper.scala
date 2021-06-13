package amf.testing.common.utils

import amf.client.environment.AMLConfiguration
import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.core.internal.parser.ParseConfiguration
import amf.core.internal.remote.Aml
import amf.core.internal.remote.Vendor.AML
import amf.core.client.scala.transform.pipelines.{TransformationPipeline, TransformationPipelineRunner}
import amf.core.internal.unsafe.PlatformBuilder.platform
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}

import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.resolution.pipelines.DialectTransformationPipeline

import scala.concurrent.{ExecutionContext, Future}

trait DialectRegistrationHelper {
  protected def withDialect[T](uri: String)(fn: (Dialect, AMLConfiguration) => Future[T])(
      implicit ec: ExecutionContext): Future[T] = {
    val eh            = DefaultErrorHandler()
    val configuration = AMLConfiguration.forEH(eh)
    val context       = new CompilerContextBuilder(uri, platform, configuration.parseConfiguration).build()
    for {
      baseUnit <- new AMFCompiler(context, Some(Aml.mediaType)).build()
      dialect  <- Future.successful { baseUnit.asInstanceOf[Dialect] }
      resolved <- Future.successful(
          TransformationPipelineRunner(DefaultErrorHandler())
            .run(dialect, DialectTransformationPipeline())
            .asInstanceOf[Dialect])
      dialectConfig <- Future.successful(configuration.withDialect(resolved))
      result        <- fn(resolved, dialectConfig)
    } yield {
      result
    }
  }
}
