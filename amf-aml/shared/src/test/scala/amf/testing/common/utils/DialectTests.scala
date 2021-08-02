package amf.testing.common.utils

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}
import amf.core.internal.remote.Mimes._
import amf.core.internal.remote._
import amf.core.internal.render.AMFSerializer
import amf.core.io.FileAssertionTest
import amf.testing.common.jsonld.MultiJsonLDAsyncFunSuite
import org.scalatest.Assertion

import scala.concurrent.Future

trait DialectTests
    extends MultiJsonLDAsyncFunSuite
    with FileAssertionTest
    with AMLParsingHelper
    with DialectRegistrationHelper {
  val basePath: String

  protected def cycleWithDialect(dialect: String,
                                 source: String,
                                 golden: String,
                                 mediaType: Option[String],
                                 directory: String = basePath,
                                 renderOptions: Option[RenderOptions] = None,
                                 baseConfig: AMLConfiguration = AMLConfiguration.predefined()): Future[Assertion] = {
    withDialect(s"file://$directory/$dialect") { (_, configuration) =>
      val config     = renderOptions.fold(configuration)(r => configuration.withRenderOptions(r))
      val nextConfig = config.merge(baseConfig)
      cycle(source, golden, mediaType, nextConfig, directory = directory)
    }
  }

  protected def parseInstance(dialect: String, source: String, directory: String = basePath): Future[BaseUnit] = {

    withDialect(s"file://$directory/$dialect") { (_, configuration) =>
      val context =
        new CompilerContextBuilder(s"file://$directory/$source", platform, configuration.compilerConfiguration)
          .build()
      new AMFCompiler(context).build()
    }
  }

  def vendorToSyntax(vendor: Spec): String = {

    vendor match {
      case Amf                   => `application/ld+json`
      case Payload               => `application/yaml`
      case Raml10 | Raml08 | Aml => `application/yaml`
      case Oas20 | Oas30         => `application/json`
      case _                     => `text/plain`
    }
  }

  override def defaultRenderOptions: RenderOptions = RenderOptions().withPrettyPrint.withSourceMaps

  final def cycle(source: String,
                  golden: String,
                  mediaType: Option[String],
                  amlConfig: AMLConfiguration = AMLConfiguration.predefined(),
                  directory: String = basePath): Future[Assertion] = {

    for {
      b <- parse(s"file://$directory/$source", platform, amlConfig)
      t <- Future.successful { transform(b, amlConfig) }
      s <- Future.successful { new AMFSerializer(t, amlConfig.renderConfiguration, mediaType).renderToString }
      d <- writeTemporaryFile(s"$directory/$golden")(s)
      r <- assertDifferences(d, s"$directory/$golden")
    } yield r

  }
  def transform(unit: BaseUnit, amlConfig: AMLConfiguration): BaseUnit = unit
}
