package amf.dialects

import amf.client.environment.AmlEnvironment
import amf.client.parse.DefaultParserErrorHandler
import amf.core.emitter.RenderOptions
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.io.{FileAssertionTest, MultiJsonldAsyncFunSuite}
import amf.core.model.document.BaseUnit
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote.Syntax.Syntax
import amf.core.remote._
import amf.core.{AMFCompiler, AMFSerializer, CompilerContextBuilder}
import amf.plugins.document.graph.AMFGraphPlugin
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.features.validation.AMFValidatorPlugin
import amf.plugins.syntax.SYamlSyntaxPlugin
import org.scalatest.{Assertion, AsyncFunSuite, BeforeAndAfterAll}

import scala.concurrent.{ExecutionContext, Future}

trait DialectTests
    extends MultiJsonldAsyncFunSuite
    with FileAssertionTest
    with DialectHelper
    with DefaultAmfInitialization {
  val basePath: String

  protected def withDialect(dialect: String,
                            source: String,
                            golden: String,
                            hint: Hint,
                            target: Vendor,
                            directory: String = basePath,
                            renderOptions: Option[RenderOptions] = None,
                            useAmfJsonldSerialization: Boolean = true): Future[Assertion] = {
    val context =
      new CompilerContextBuilder(s"file://$directory/$dialect", platform, DefaultParserErrorHandler.withRun())
        .build(AmlEnvironment.aml())
    for {
      dialect <- new AMFCompiler(context, None, Some(Aml.name)).build()
      _       <- Future.successful { AMLPlugin().resolve(dialect, UnhandledErrorHandler) }
      res <- cycle(source,
                   golden,
                   hint,
                   target,
                   useAmfJsonldSerialization = useAmfJsonldSerialization,
                   renderOptions = renderOptions,
                   directory = directory)
    } yield {
      res
    }
  }

  protected def parseInstance(dialect: String,
                              source: String,
                              hint: Hint,
                              directory: String = basePath): Future[BaseUnit] = {
    val env = AmlEnvironment.aml()
    for {
      dialect <- new AMFCompiler(new CompilerContextBuilder(s"file://$directory/$dialect",
                                                            platform,
                                                            DefaultParserErrorHandler.withRun()).build(env),
                                 None,
                                 Some(Aml.name)).build()
      _ <- Future.successful { AMLPlugin().resolve(dialect, UnhandledErrorHandler) }
      b <- new AMFCompiler(new CompilerContextBuilder(s"file://$directory/$source",
                                                      platform,
                                                      DefaultParserErrorHandler.withRun()).build(env),
                           None,
                           Some(hint.vendor.name)).build()
    } yield {
      b
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
                  useAmfJsonldSerialization: Boolean = true,
                  syntax: Option[Syntax] = None): Future[Assertion] = {
    val options = renderOptions.getOrElse(defaultRenderOptions)
    if (!useAmfJsonldSerialization) options.withoutAmfJsonLdSerialization else options.withAmfJsonLdSerialization
    for {
      b <- parseAndRegisterDialect(s"file://$directory/$source", platform, hint)
      t <- Future.successful { transform(b) }
      s <- new AMFSerializer(t, vendorToSyntax(target), target.name, options)
        .renderToString(scala.concurrent.ExecutionContext.Implicits.global)
      d <- writeTemporaryFile(s"$directory/$golden")(s)
      r <- assertDifferences(d, s"$directory/$golden")
    } yield r

  }
  def transform(unit: BaseUnit): BaseUnit = unit
}

abstract class DialectInstanceResolutionCycleTests extends DialectTests {
  override def transform(unit: BaseUnit): BaseUnit =
    AMLPlugin().resolve(unit, UnhandledErrorHandler)
}
