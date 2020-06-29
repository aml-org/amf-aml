package amf.dialects

import amf.client.parse.DefaultParserErrorHandler
import amf.core.emitter.RenderOptions
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.io.FileAssertionTest
import amf.core.model.document.BaseUnit
import amf.core.remote.Syntax.Syntax
import amf.core.remote._
import amf.core.unsafe.PlatformSecrets
import amf.core.{AMFCompiler, AMFSerializer, CompilerContextBuilder}
import amf.plugins.document.graph.AMFGraphPlugin
import amf.plugins.document.graph.parser.{ExpandedForm, FlattenedForm, JsonLdDocumentForm}
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.syntax.SYamlSyntaxPlugin
import org.scalactic.Fail
import org.scalatest.{Assertion, AsyncFunSuite}

import scala.concurrent.{ExecutionContext, Future}

abstract class MultiJsonldAsyncFunSuite extends AsyncFunSuite {
  def testedForms: Seq[JsonLdDocumentForm] = Seq(FlattenedForm, ExpandedForm)

  def defaultRenderOptions: RenderOptions = RenderOptions()

  def renderOptionsFor(documentForm: JsonLdDocumentForm): RenderOptions = {
    documentForm match {
      case FlattenedForm => defaultRenderOptions.withFlattenedJsonLd
      case ExpandedForm  => defaultRenderOptions.withoutFlattenedJsonLd
      case _             => defaultRenderOptions

    }
  }

  private def validatePattern(pattern: String, patternName: String): Unit = {
    if (!pattern.contains("%s")) {
      Fail(s"$pattern is not a valid $patternName pattern. Must contain %s as the handled JSON-LD extension")
    }
  }

  // Single source, multiple JSON-LD outputs
  def multiGoldenTest(testText: String, goldenNamePattern: String)(
      testFn: MultiGoldenTestConfig => Future[Assertion]): Unit = {
    testedForms.foreach { form =>
      validatePattern(goldenNamePattern, "goldenNamePattern")
      val golden = goldenNamePattern.format(form.extension)
      val config = MultiGoldenTestConfig(golden, renderOptionsFor(form))
      test(s"$testText for ${form.name} JSON-LD golden")(testFn(config))
    }
  }

  // Multiple JSON-LD sources, single output
  def multiSourceTest(testText: String, sourceNamePattern: String)(
      testFn: MultiSourceTestConfig => Future[Assertion]): Unit = {
    testedForms.foreach { form =>
      validatePattern(sourceNamePattern, "sourceNamePattern")
      val source = sourceNamePattern.format(form.extension)
      val config = MultiSourceTestConfig(source)
      test(s"$testText for ${form.name} JSON-LD source")(testFn(config))
    }
  }

  // Multiple JSON-LD sources, multiple JSON-LD outputs. Each source matches exactly one output
  def multiTest(testText: String, sourceNamePattern: String, goldenNamePattern: String)(
      testFn: MultiTestConfig => Future[Assertion]): Unit = {
    testedForms.foreach { form =>
      validatePattern(sourceNamePattern, "sourceNamePattern")
      validatePattern(goldenNamePattern, "goldenNamePattern")
      val source = sourceNamePattern.format(form.extension)
      val golden = goldenNamePattern.format(form.extension)
      val config = MultiTestConfig(source, golden, renderOptionsFor(form))
      test(s"$testText for ${form.name} JSON-LD")(testFn(config))
    }
  }
}

case class MultiGoldenTestConfig(golden: String, renderOptions: RenderOptions)
case class MultiSourceTestConfig(source: String)
case class MultiTestConfig(source: String, golden: String, renderOptions: RenderOptions)

trait DialectTests extends MultiJsonldAsyncFunSuite with FileAssertionTest {
  val basePath: String

  def init(): Future[Unit] = {
    amf.core.AMF.init().map { _ =>
      amf.core.registries.AMFPluginsRegistry.registerSyntaxPlugin(SYamlSyntaxPlugin)
      amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(AMFGraphPlugin)
      amf.core.registries.AMFPluginsRegistry.registerDocumentPlugin(AMLPlugin)
    }
  }

  protected def withDialect(dialect: String,
                            source: String,
                            golden: String,
                            hint: Hint,
                            target: Vendor,
                            directory: String = basePath,
                            renderOptions: Option[RenderOptions] = None,
                            useAmfJsonldSerialization: Boolean = true): Future[Assertion] = {
    val context =
      new CompilerContextBuilder(s"file://$directory/$dialect", platform, DefaultParserErrorHandler.withRun()).build()
    for {
      _       <- init()
      dialect <- new AMFCompiler(context, None, Some(Aml.name)).build()
      _       <- Future { AMLPlugin().resolve(dialect, UnhandledErrorHandler) }
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
    for {
      _ <- init()
      dialect <- new AMFCompiler(new CompilerContextBuilder(s"file://$directory/$dialect",
                                                            platform,
                                                            DefaultParserErrorHandler.withRun()).build(),
                                 None,
                                 Some(Aml.name)).build()
      _ <- Future { AMLPlugin().resolve(dialect, UnhandledErrorHandler) }
      b <- new AMFCompiler(new CompilerContextBuilder(s"file://$directory/$source",
                                                      platform,
                                                      DefaultParserErrorHandler.withRun()).build(),
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

  final def parseAndRegisterDialect(uri: String, platform: Platform, hint: Hint): Future[BaseUnit] =
    for {
      _ <- init()
      r <- new AMFCompiler(new CompilerContextBuilder(uri, platform, DefaultParserErrorHandler.withRun()).build(),
                           None,
                           Some(hint.vendor.name))
        .build()
    } yield {
      r
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
      t <- Future { transform(b) }
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


