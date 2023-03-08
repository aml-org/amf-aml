package amf.testing.resolution

import amf.core.internal.remote.Mimes.`application/yaml`
import amf.core.internal.remote.Syntax.Yaml
import amf.core.internal.remote.{Amf, Aml, Mimes, VocabularyYamlHint}
import amf.core.internal.unsafe.PlatformSecrets

import scala.concurrent.ExecutionContext

class DialectInstanceResolutionTest extends DialectInstanceResolutionCycleTests with PlatformSecrets {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/instances/"

  test("resolve fragment test") {
    cycleWithDialect("dialect8.yaml", "example8.yaml", "example8.resolved.yaml", mediaType = Some(`application/yaml`))
  }

  test("resolve library test") {
    cycleWithDialect("dialect9.yaml", "example9.yaml", "example9.resolved.yaml", mediaType = Some(`application/yaml`))
  }

  test("resolve patch 22a test") {
    cycleWithDialect("dialect22.yaml", "patch22.yaml", "patch22.resolved.yaml", mediaType = Some(`application/yaml`))
  }

  test("resolve patch 22b test") {
    cycleWithDialect("dialect22.yaml", "patch22b.yaml", "patch22b.resolved.yaml", mediaType = Some(`application/yaml`))
  }

  test("resolve patch 22c test") {
    cycleWithDialect("dialect22.yaml", "patch22c.yaml", "patch22c.resolved.yaml", mediaType = Some(`application/yaml`))
  }

  test("resolve patch 22d test") {
    cycleWithDialect("dialect22.yaml", "patch22d.yaml", "patch22d.resolved.yaml", mediaType = Some(`application/yaml`))
  }

  test("Resolve patch properties to AML") {
    cycleWithDialect(
      "dialect.yaml",
      "patch.yaml",
      "patch.resolved.yaml",
      Some(`application/yaml`),
      directory = s"$basePath/patch-properties/"
    )
  }

  multiGoldenTest("Resolve declares on self-encoded dialect instance", "instance.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "instance.yaml",
      config.golden,
      Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/declares-in-self-encoded/"
    )
  }

  multiGoldenTest("Resolve patch properties to AMF Graph", "patch.resolved.%s") { config =>
    cycleWithDialect(
      "dialect.yaml",
      "patch.yaml",
      config.golden,
      Some(Mimes.`application/ld+json`),
      renderOptions = Some(config.renderOptions),
      directory = s"$basePath/patch-properties/"
    )
  }

}
