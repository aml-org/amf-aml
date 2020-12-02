package amf.dialects
import amf.core.emitter.RenderOptions
import amf.plugins.document.graph.parser.{ExpandedForm, FlattenedForm, JsonLdDocumentForm}
import org.scalactic.Fail
import org.scalatest.{Assertion, AsyncFunSuite}

import scala.concurrent.Future

// Multi test configs
class MultiJsonLdDocumentFormTest(jsonLdDocumentForm: JsonLdDocumentForm)

case class MultiTestConfig(source: String,
                           golden: String,
                           renderOptions: RenderOptions,
                           jsonLdDocumentForm: JsonLdDocumentForm)
    extends MultiJsonLdDocumentFormTest(jsonLdDocumentForm)

case class MultiSourceTestConfig(source: String, jsonLdDocumentForm: JsonLdDocumentForm)
    extends MultiJsonLdDocumentFormTest(jsonLdDocumentForm)

case class MultiGoldenTestConfig(golden: String, renderOptions: RenderOptions, jsonLdDocumentForm: JsonLdDocumentForm)
    extends MultiJsonLdDocumentFormTest(jsonLdDocumentForm)

// Multi JSON-LD abstract test suite
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
      val config = MultiGoldenTestConfig(golden, renderOptionsFor(form), form)
      test(s"$testText for ${form.name} JSON-LD golden")(testFn(config))
    }
  }

  // Multiple JSON-LD sources, single output
  def multiSourceTest(testText: String, sourceNamePattern: String)(
      testFn: MultiSourceTestConfig => Future[Assertion]): Unit = {
    testedForms.foreach { form =>
      validatePattern(sourceNamePattern, "sourceNamePattern")
      val source = sourceNamePattern.format(form.extension)
      val config = MultiSourceTestConfig(source, form)
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
      val config = MultiTestConfig(source, golden, renderOptionsFor(form), form)
      test(s"$testText for ${form.name} JSON-LD")(testFn(config))
    }
  }
}
