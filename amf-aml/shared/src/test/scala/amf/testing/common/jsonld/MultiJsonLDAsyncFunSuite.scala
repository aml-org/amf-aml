package amf.testing.common.jsonld

import amf.core.client.scala.config.RenderOptions
import amf.core.internal.plugins.document.graph.{EmbeddedForm, FlattenedForm, JsonLdDocumentForm}
import amf.testing.common.jsonld
import org.scalactic.Fail
import org.scalatest.Assertion
import org.scalatest.funsuite.AsyncFunSuite

import scala.concurrent.Future

/**
  * Cycle tests using temporary file and directory creator
  */
abstract class MultiJsonLDAsyncFunSuite extends AsyncFunSuite {
  def testedForms: Seq[JsonLdDocumentForm] = Seq(FlattenedForm, EmbeddedForm)

  def defaultRenderOptions: RenderOptions = RenderOptions()

  def renderOptionsFor(documentForm: JsonLdDocumentForm): RenderOptions = {
    documentForm match {
      case FlattenedForm => defaultRenderOptions.withFlattenedJsonLd
      case EmbeddedForm  => defaultRenderOptions.withoutFlattenedJsonLd
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
      testFn: WithJsonLDGoldenConfig => Future[Assertion]): Unit = {
    testedForms.foreach { form =>
      validatePattern(goldenNamePattern, "goldenNamePattern")
      val golden = goldenNamePattern.format(form.extension)
      val config = jsonld.WithJsonLDGoldenConfig(golden, renderOptionsFor(form), form)
      test(s"$testText for ${form.name} JSON-LD golden")(testFn(config))
    }
  }

  // Multiple JSON-LD sources, single output
  def multiSourceTest(testText: String, sourceNamePattern: String)(
      testFn: WithJsonLDSourceConfig => Future[Assertion]): Unit = {
    testedForms.foreach { form =>
      validatePattern(sourceNamePattern, "sourceNamePattern")
      val source = sourceNamePattern.format(form.extension)
      val config = jsonld.WithJsonLDSourceConfig(source, form)
      test(s"$testText for ${form.name} JSON-LD source")(testFn(config))
    }
  }

  // Multiple JSON-LD sources, multiple JSON-LD outputs. Each source matches exactly one output
  def multiTest(testText: String, sourceNamePattern: String, goldenNamePattern: String)(
      testFn: WithJsonLDGoldenAndSourceConfig => Future[Assertion]): Unit = {
    testedForms.foreach { form =>
      validatePattern(sourceNamePattern, "sourceNamePattern")
      validatePattern(goldenNamePattern, "goldenNamePattern")
      val source = sourceNamePattern.format(form.extension)
      val golden = goldenNamePattern.format(form.extension)
      val config = jsonld.WithJsonLDGoldenAndSourceConfig(source, golden, renderOptionsFor(form), form)
      test(s"$testText for ${form.name} JSON-LD")(testFn(config))
    }
  }
}
