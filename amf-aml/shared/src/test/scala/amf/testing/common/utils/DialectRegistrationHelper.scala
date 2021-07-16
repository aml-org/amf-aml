package amf.testing.common.utils

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.document.Dialect
import amf.aml.internal.transform.pipelines.DialectTransformationPipeline
import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.core.client.scala.transform.TransformationPipelineRunner
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}
import amf.core.internal.remote.Aml
import amf.core.internal.unsafe.PlatformBuilder.platform

import scala.concurrent.{ExecutionContext, Future}

trait DialectRegistrationHelper {
  protected def withDialect[T](uri: String)(fn: (Dialect, AMLConfiguration) => Future[T])(
      implicit ec: ExecutionContext): Future[T] = {
    val eh            = DefaultErrorHandler()
    val configuration = AMLConfiguration.forEH(eh)
    val context       = new CompilerContextBuilder(uri, platform, configuration.compilerConfiguration).build()
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
