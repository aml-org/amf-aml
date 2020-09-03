package amf.dialects

import amf.core.emitter.RenderOptions
import amf.core.remote.{Amf, AmfJsonHint, Aml, VocabularyYamlHint}
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class JapaneseVocabularyParsingTest extends DialectTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "shared/src/test/resources/vocabularies2/japanese/dialects/"

  multiGoldenTest("parse 1 test", "example1.%s") { config =>
    init().flatMap(
      _ =>
        cycle("example1.yaml",
              config.golden,
              VocabularyYamlHint,
              target = Amf,
              renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 2 test", "example2.%s") { config =>
    init().flatMap(
      _ =>
        cycle("example2.yaml",
              config.golden,
              VocabularyYamlHint,
              target = Amf,
              renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 3 test", "example3.%s") { config =>
    init().flatMap(
      _ =>
        cycle("example3.yaml",
              config.golden,
              VocabularyYamlHint,
              target = Amf,
              renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 4 test", "example4.%s") { config =>
    init().flatMap(
      _ =>
        cycle("example4.yaml",
              config.golden,
              VocabularyYamlHint,
              target = Amf,
              renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 5 test", "example5.%s") { config =>
    init().flatMap(
      _ =>
        cycle("example5.yaml",
              config.golden,
              VocabularyYamlHint,
              target = Amf,
              renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 6 test", "example6.%s") { config =>
    init().flatMap(
      _ =>
        cycle("example6.yaml",
              config.golden,
              VocabularyYamlHint,
              target = Amf,
              renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("parse 7 test", "example7.%s") { config =>
    init().flatMap(
      _ =>
        cycle("example7.yaml",
              config.golden,
              VocabularyYamlHint,
              target = Amf,
              renderOptions = Some(config.renderOptions)))
  }

  multiSourceTest("generate 1 test", "example1.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example1.yaml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 2 test", "example2.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example2.yaml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 3 test", "example3.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example3.yaml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 4 test", "example4.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example4.yaml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 5 test", "example5.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example5.yaml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 6 test", "example6.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example6.yaml", AmfJsonHint, target = Aml))
  }

  multiSourceTest("generate 7 test", "example7.%s") { config =>
    init().flatMap(_ => cycle(config.source, "example7.yaml", AmfJsonHint, target = Aml))
  }

  override def defaultRenderOptions: RenderOptions = RenderOptions().withSourceMaps.withPrettyPrint
}
