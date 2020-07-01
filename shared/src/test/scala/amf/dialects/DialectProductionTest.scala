package amf.dialects

import amf.core.remote._
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.ExecutionContext

class DialectProductionTest extends DialectTests with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = init()

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  override val basePath = "shared/src/test/resources/vocabularies2/production/"

  ignore("Can parse the canonical webapi dialect") {
    cycle("canonical_webapi.yaml", "canonical_webapi.json", VocabularyYamlHint, target = Amf, "vocabularies/dialects/")
  }

  multiGoldenTest("Can parse validation dialect eee", "validation_dialect.%s") { config =>
    cycle("validation_dialect.raml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions))
  }

  test("Can parse validation dialect instance") {
    withDialect("validation_dialect.raml",
                "validation_instance1.raml",
                "validation_instance1.raml.raml",
                VocabularyYamlHint,
                target = Aml)
  }

  multiGoldenTest("Can parse validation dialect cfg1 instance", "example1_instance.%s") { config =>
    withDialect(
        "example1.raml",
        "example1_instance.raml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = s"${basePath}cfg/",
        renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("Can parse validation dialect cfg2 instance", "example2_instance.%s") { config =>
    withDialect(
        "example2.raml",
        "example2_instance.raml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = s"${basePath}cfg/",
        renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("Can parse validation dialect cfg3 instance", "example3_instance.%s") { config =>
    withDialect(
        "example3.raml",
        "example3_instance.raml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = s"${basePath}cfg/",
        renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("Can parse ABOUT dialect", "ABOUT-dialect.%s") { config =>
    cycle("ABOUT-dialect.raml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          s"${basePath}ABOUT/",
          renderOptions = Some(config.renderOptions))
  }

  test("Can parse and generated ABOUT dialect") {
    cycle("ABOUT-dialect.raml",
          "ABOUT-dialect.raml.raml",
          VocabularyYamlHint,
          target = Aml,
          directory = s"${basePath}ABOUT/")
  }

  test("Can parse and generate ABOUT dialect instance") {
    withDialect("ABOUT-dialect.raml",
                "ABOUT.yaml",
                "ABOUT.yaml.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = s"${basePath}ABOUT/")
  }

  test("Can parse and generate ABOUT-github dialect instance") {
    withDialect("ABOUT-GitHub-dialect.raml",
                "example.yaml",
                "example.yaml.raml",
                VocabularyYamlHint,
                target = Aml,
                directory = s"${basePath}ABOUT/github/")
  }

  multiGoldenTest("Can parse ABOUT-hosted dialect instance", "ABOUT_hosted.%s") { config =>
    withDialect(
        "ABOUT-hosted-vcs-dialect.yaml",
        "ABOUT_hosted.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = s"${basePath}ABOUT/",
        renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("Can parse and generate the Instagram dialect", "dialect.%s") { config =>
    cycle("dialect.raml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          s"${basePath}Instagram/",
          renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("Can parse and generate Instance dialect instance 1", "instance1.%s") { config =>
    withDialect("dialect.raml",
                "instance1.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                s"${basePath}Instagram/",
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("Can parse and generate Instance dialect instance 2", "instance2.%s") { config =>
    withDialect("dialect.raml",
                "instance2.raml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                s"${basePath}Instagram/",
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("Can parse and generate the activity dialect", "activity.%s") { config =>
    cycle("activity.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          s"${basePath}streams/",
          renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("Can parse activity instances", "stream1.%s") { config =>
    withDialect("activity.yaml",
                "stream1.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                s"${basePath}streams/",
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("Can parse activity deployments demo", "deployment.%s") { config =>
    withDialect("dialect.yaml",
                "deployment.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                s"${basePath}deployments_demo/",
                renderOptions = Some(config.renderOptions))
  }

  multiGoldenTest("Can parse guids example", "guids_instance.%s") { config =>
    withDialect("guids_dialect.yaml",
                "guids_instance.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                s"${basePath}guids/",
                renderOptions = Some(config.renderOptions))
  }

}

class DialectProductionResolutionTest extends DialectInstanceResolutionCycleTests {

  override val basePath = "shared/src/test/resources/vocabularies2/production/"

  // Order is not predictable
  ignore("Can parse asyncapi overlay instances") {
    withDialect("dialect6.yaml",
                "patch6.yaml",
                "patch6.resolved.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = s"${basePath}asyncapi/")
  }
}
