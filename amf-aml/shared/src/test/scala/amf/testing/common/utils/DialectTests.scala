package amf.testing.common.utils

import amf.client.environment.AMLConfiguration
import amf.client.remod.ParseConfiguration
import amf.core.emitter.RenderOptions
import amf.core.io.FileAssertionTest
import amf.core.model.document.BaseUnit
import amf.core.remote._
import amf.core.{AMFCompiler, AMFSerializer, CompilerContextBuilder}
import amf.testing.common.jsonld.MultiJsonLDAsyncFunSuite
import org.scalatest.Assertion

import scala.concurrent.Future

trait DialectTests
    extends MultiJsonLDAsyncFunSuite
    with FileAssertionTest
    with AMLParsingHelper
    with DefaultAMLInitialization
    with DialectRegistrationHelper {
  val basePath: String

  protected def cycleWithDialect(dialect: String,
                                 source: String,
                                 golden: String,
                                 hint: Hint,
                                 target: Vendor,
                                 directory: String = basePath,
                                 renderOptions: Option[RenderOptions] = None,
                                 useAmfJsonldSerialization: Boolean = true): Future[Assertion] = {
    withDialect(s"file://$directory/$dialect") { (_, configuration) =>
      cycle(source,
            golden,
            hint,
            target,
            configuration,
            useAmfJsonldSerialization = useAmfJsonldSerialization,
            renderOptions = renderOptions,
            directory = directory)
    }
  }

  protected def parseInstance(dialect: String,
                              source: String,
                              hint: Hint,
                              directory: String = basePath): Future[BaseUnit] = {

    withDialect(s"file://$directory/$dialect") { (_, configuration) =>
      val context =
        new CompilerContextBuilder(s"file://$directory/$source", platform, ParseConfiguration(configuration))
          .build()
      new AMFCompiler(context, Some(hint.vendor.mediaType)).build()
    }
  }

  def vendorToSyntax(vendor: Vendor): String = {

    vendor match {
      case Amf                   => "application/ld+json"
      case Payload               => "application/amf+json"
      case Raml10 | Raml08 | Aml => "application/yaml"
      case Oas20 | Oas30         => "application/json"
      case _                     => "text/plain"
    }
  }

  override def defaultRenderOptions: RenderOptions = RenderOptions().withPrettyPrint.withSourceMaps

  final def cycle(source: String,
                  golden: String,
                  hint: Hint,
                  target: Vendor,
                  amlConfig: AMLConfiguration = AMLConfiguration.predefined(),
                  directory: String = basePath,
                  renderOptions: Option[RenderOptions] = None,
                  useAmfJsonldSerialization: Boolean = true): Future[Assertion] = {
    val options = renderOptions.getOrElse(defaultRenderOptions)
    if (!useAmfJsonldSerialization) options.withoutAmfJsonLdSerialization else options.withAmfJsonLdSerialization

    for {
      b <- parse(s"file://$directory/$source", platform, hint, amlConfig)
      t <- Future.successful { transform(b) }
      s <- new AMFSerializer(t, vendorToSyntax(target), target.name, options)
        .renderToString(scala.concurrent.ExecutionContext.Implicits.global)
      d <- writeTemporaryFile(s"$directory/$golden")(s)
      r <- assertDifferences(d, s"$directory/$golden")
    } yield r

  }
  def transform(unit: BaseUnit): BaseUnit = unit
}
