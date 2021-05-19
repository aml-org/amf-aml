package amf.testing.unclassified

import amf.client.environment.AMLConfiguration
import amf.client.remod.amfcore.config.RenderOptions
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.remote.{Vendor, VocabularyYamlHint}
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.semantic.{
  NameFieldExtractor,
  PropertyTermFieldExtractor,
  SemanticExtensionFinder,
  TargetFieldExtractor
}
import amf.testing.common.cycling.BuildCycleTestCommon
import amf.testing.common.utils.DefaultAMLInitialization
import org.scalatest.OptionValues._
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class SemanticExtensionFinderTest
    extends AsyncFunSuite
    with BuildCycleTestCommon
    with Matchers
    with DefaultAMLInitialization {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  override def basePath: String =
    "amf-aml/shared/src/test/resources/vocabularies2/dialects/annotation-mappings-finding/"
  private val DIALECT = "dialect.yaml"

  test("Find Semantic Extensions By Name") {
    withFinder(DIALECT, byNameFinder) { finder =>
      finder.find("maintainer").headOption.value.extensionName().option().value should equal("maintainer")
      finder.find("contributor").headOption.value.extensionName().option().value should equal("contributor")
      finder.find("nonexistent") shouldBe empty
    }
  }

  test("Find Semantic Extensions by target") {
    withFinder(DIALECT, byTargetFinder) { finder =>
      finder.find("http://a.ml/vocab#API").map(_.extensionName().value()) should contain theSameElementsAs Seq(
          "maintainer",
          "contributor")
      finder.find("http://a.ml/vocab#Operation") shouldBe empty
    }
  }

  test("Find Semantic Extension by property ter,") {
    withFinder(DIALECT, byPropertyTerm) { finder =>
      finder
        .find("http://github.org/vocabulary#contributor")
        .map(_.extensionName().value()) should contain theSameElementsAs Seq("contributor")
      finder.find("http://a.ml/vocab#maintainer").map(_.extensionName().value()) should contain theSameElementsAs Seq(
          "maintainer")
      finder.find("http://github.org/vocabulary#maintainer") shouldBe empty
    }
  }

  private def withFinder[T](path: String, factoryFunc: Dialect => SemanticExtensionFinder)(
      block: SemanticExtensionFinder => T): Future[T] = {
    for {
      dialect <- parseDialect(path)
    } yield {
      val finder = factoryFunc(dialect)
      block(finder)
    }
  }

  private def byPropertyTerm(dialect: Dialect): SemanticExtensionFinder =
    SemanticExtensionFinder(dialect.extensions(), new PropertyTermFieldExtractor(dialect))
  private def byTargetFinder(dialect: Dialect): SemanticExtensionFinder =
    SemanticExtensionFinder(dialect.extensions(), new TargetFieldExtractor(dialect))
  private def byNameFinder(dialect: Dialect): SemanticExtensionFinder =
    SemanticExtensionFinder(dialect.extensions(), NameFieldExtractor)

  private def parseDialect(path: String): Future[Dialect] = {
    val config = CycleConfig(path, "", VocabularyYamlHint, Vendor.AML)
    val amlConfig = AMLConfiguration
      .predefined()
      .withRenderOptions(RenderOptions().withAmfJsonLdSerialization)
      .withErrorHandlerProvider(() => UnhandledErrorHandler)
    build(config, amlConfig).map(_.asInstanceOf[Dialect])
  }
}
