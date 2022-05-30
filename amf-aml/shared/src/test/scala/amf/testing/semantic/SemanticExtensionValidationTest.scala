package amf.testing.semantic

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.{DefaultErrorHandlerProvider, UnhandledErrorHandler}
import amf.testing.common.utils.{
  DialectInstanceValidation,
  MultiPlatformReportComparator,
  ReportComparator,
  UniquePlatformReportComparator
}
import org.scalatest.Assertion

import scala.concurrent.{ExecutionContext, Future}

class SemanticExtensionValidationTest extends DialectInstanceValidation {

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  override def basePath: String = "file://amf-aml/shared/src/test/resources/vocabularies2/semantic/validation/"

  test("Semantic extensions without mandatory properties should fail") {
    getConfig(Seq("object-extensions.yaml", "scalar-extensions.yaml")).flatMap { config =>
      validate(
          "dialect.yaml",
          "instances/invalid-obj-instance.yaml",
          Some("reports/invalid-obj-report.report"),
          config = config
      )
    }
  }

  test("Semantic extensions without valid minimum or maximum should fail") {
    getConfig(Seq("object-extensions.yaml", "scalar-extensions.yaml")).flatMap { config =>
      validate(
          "dialect.yaml",
          "instances/invalid-ranking-instance.yaml",
          Some("reports/invalid-ranking-report.report"),
          config = config,
          comparator = MultiPlatformReportComparator
      )
    }
  }

  test("Semantic extensions with boolean ranking should fail") {
    getConfig(Seq("object-extensions.yaml", "scalar-extensions.yaml")).flatMap { config =>
      validate(
          "dialect.yaml",
          "instances/invalid-ranking-type-instance.yaml",
          Some("reports/invalid-ranking-type-report.report"),
          config = config
      )
    }
  }

  test("Semantic extensions with valid minimum or maximum") {
    getConfig(Seq("object-extensions.yaml", "scalar-extensions.yaml")).flatMap { config =>
      validate("dialect.yaml", "instances/valid-ranking-instance.yaml", None, config = config)
    }
  }

  test("Unresolved links should be validated") {
    getConfig(Seq("object-extensions.yaml")).flatMap { config =>
      validate(
          "dialect.yaml",
          "instances/invalid-obj-link.yaml",
          Some("reports/invalid-obj-link.report"),
          config = config
      )
    }
  }

  private def getConfig(dialects: Seq[String]): Future[AMLConfiguration] = {
    val config = AMLConfiguration
      .predefined()
      .withRenderOptions(RenderOptions().withPrettyPrint.withCompactUris)
      .withErrorHandlerProvider(() => UnhandledErrorHandler)
    val loadedConfig = dialects.foldLeft(Future.successful(config)) { (acc, curr) =>
      acc.flatMap(config => config.withDialect(basePath + curr))
    }
    loadedConfig.map(_.withErrorHandlerProvider(DefaultErrorHandlerProvider))
  }

  private def validate(
      dialect: String,
      instance: String,
      golden: Option[String] = None,
      path: String = basePath,
      config: AMLConfiguration,
      comparator: ReportComparator = UniquePlatformReportComparator
  ): Future[Assertion] = {
    validation(dialect, instance, path, config) flatMap {
      comparator.assertReport(_, golden.map(g => s"$path/$g"), jsonldReport = false)
    }
  }
}
