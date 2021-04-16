package amf.testing.common.utils

import amf.client.parse.DefaultParserErrorHandler
import amf.core.emitter.RenderOptions
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.io.FileAssertionTest
import amf.core.model.document.BaseUnit
import amf.core.remote.Syntax.Syntax
import amf.core.remote._
import amf.core.{AMFCompiler, AMFSerializer, CompilerContextBuilder}
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.Dialect
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

    withDialect(s"file://$directory/$dialect") { _ =>
      cycle(source,
            golden,
            hint,
            target,
            useAmfJsonldSerialization = useAmfJsonldSerialization,
            renderOptions = renderOptions,
            directory = directory)
    }
  }

  protected def parseInstance(dialect: String,
                              source: String,
                              hint: Hint,
                              directory: String = basePath): Future[BaseUnit] = {

    withDialect(s"file://$directory/$dialect") { _ =>
      val context =
        new CompilerContextBuilder(s"file://$directory/$source", platform, DefaultParserErrorHandler.withRun()).build()
      new AMFCompiler(context, None, Some(hint.vendor.name)).build()
    }
  }

  def vendorToSyntax(vendor: Vendor): String = {

    vendor match {
      case Amf                          => "application/ld+json"
      case Payload                      => "application/amf+json"
      case Raml10 | Raml08 | Raml | Aml => "application/yaml"
      case Oas | Oas20 | Oas30          => "application/json"
      case _                            => "text/plain"
    }
  }

  override def defaultRenderOptions: RenderOptions = RenderOptions().withPrettyPrint.withSourceMaps

  final def cycle(source: String,
                  golden: String,
                  hint: Hint,
                  target: Vendor,
                  directory: String = basePath,
                  renderOptions: Option[RenderOptions] = None,
                  useAmfJsonldSerialization: Boolean = true): Future[Assertion] = {
    val options = renderOptions.getOrElse(defaultRenderOptions)
    if (!useAmfJsonldSerialization) options.withoutAmfJsonLdSerialization else options.withAmfJsonLdSerialization
    for {
      b <- parse(s"file://$directory/$source", platform, hint)
      t <- Future.successful { transform(b) }
      s <- new AMFSerializer(t, vendorToSyntax(target), target.name, options)
        .renderToString(scala.concurrent.ExecutionContext.Implicits.global)
      d <- writeTemporaryFile(s"$directory/$golden")(s)
      r <- assertDifferences(d, s"$directory/$golden")
    } yield r

  }
  def transform(unit: BaseUnit): BaseUnit = unit
}
