package amf.dialects

import amf.core.emitter.RenderOptions
import amf.core.io.FileAssertionTest
import amf.core.{AMFCompiler, AMFSerializer}
import amf.core.model.document.BaseUnit
import amf.core.parser.UnhandledErrorHandler
import amf.core.remote.Syntax.Syntax
import amf.core.remote._
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.graph.AMFGraphPlugin
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.syntax.SYamlSyntaxPlugin
import org.scalatest.{Assertion, AsyncFunSuite}

import scala.concurrent.{ExecutionContext, Future}

trait DialectTests extends AsyncFunSuite with FileAssertionTest{
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
                            useAmfJsonldSerialization: Boolean = true): Future[Assertion] = {
    for {
      _         <- init()
      dialect   <- new AMFCompiler(s"file://$directory/$dialect", platform, None, None,Some(Aml.name), cache = Cache()).build()
      _         <- Future { AMLPlugin.resolve(dialect, UnhandledErrorHandler) }
      res       <- cycle(source, golden, hint, target, useAmfJsonldSerialization = useAmfJsonldSerialization, directory = directory)
    } yield {
      res
    }
  }

  protected def parseInstance(dialect: String,
                            source: String,
                            hint: Hint,
                            directory: String = basePath): Future[BaseUnit] = {
    for {
      _         <- init()
      dialect   <- new AMFCompiler(s"file://$directory/$dialect", platform, None, None,Some(Aml.name), cache = Cache()).build()
      _         <- Future { AMLPlugin.resolve(dialect, UnhandledErrorHandler) }
      b         <- new AMFCompiler(s"file://$directory/$source", platform, None, None,Some(hint.vendor.name), cache = Cache()).build()
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

  final def cycle(source: String,
                  golden: String,
                  hint: Hint,
                  target: Vendor,
                  directory: String = basePath,
                  useAmfJsonldSerialization: Boolean = true,
                  syntax: Option[Syntax] = None): Future[Assertion] = {
    val options = RenderOptions().withPrettyPrint.withSourceMaps
    if (!useAmfJsonldSerialization) options.withoutAmfJsonLdSerialization else options.withAmfJsonLdSerialization

    for {
      b <- new AMFCompiler(s"file://$directory/$source", platform, None, None,Some(hint.vendor.name), cache = Cache()).build()
      t <- Future{transform(b)}
      s <- new AMFSerializer(t,  vendorToSyntax(target), target.name, options)
        .renderToString(scala.concurrent.ExecutionContext.Implicits.global)
      d <- writeTemporaryFile(s"$directory/$golden")(s)
      r <- assertDifferences(d, s"$directory/$golden")
    } yield r

  }
  def transform(unit: BaseUnit): BaseUnit = unit
}

abstract class DialectInstanceResolutionCycleTests extends DialectTests {
  override def transform(unit: BaseUnit): BaseUnit =
    AMLPlugin.resolve(unit, UnhandledErrorHandler)
}

class DialectInstanceResolutionTest extends DialectInstanceResolutionCycleTests with PlatformSecrets {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "shared/src/test/resources/vocabularies2/instances/"

  test("resolve fragment test") {
    withDialect("dialect8.raml", "example8.raml", "example8.resolved.raml", VocabularyYamlHint, Aml)
  }

  test("resolve library test") {
    withDialect("dialect9.raml", "example9.raml", "example9.resolved.raml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22a test") {
    withDialect("dialect22.raml", "patch22.raml", "patch22.resolved.raml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22b test") {
    withDialect("dialect22.raml", "patch22b.raml", "patch22b.resolved.raml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22c test") {
    withDialect("dialect22.raml", "patch22c.raml", "patch22c.resolved.raml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22d test") {
    withDialect("dialect22.raml", "patch22d.raml", "patch22d.resolved.raml", VocabularyYamlHint, Aml)
  }

}
