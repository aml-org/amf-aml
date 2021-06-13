package amf.testing.parsing

import amf.client.environment.AMLConfiguration
import amf.core.internal.remote.{Amf, AmfJsonHint, Aml, VocabularyYamlHint}
import amf.testing.common.utils.DialectTests

import scala.concurrent.ExecutionContext

class VocabularyParsingTest extends DialectTests {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/vocabularies/"

  multiGoldenTest("parse 1 test", "example1.%s") { config =>
    cycle("example1.yaml", config.golden, VocabularyYamlHint, target = Amf, config.config)
  }

  multiGoldenTest("parse 2 test", "example2.%s") { config =>
    cycle("example2.yaml", config.golden, VocabularyYamlHint, target = Amf, config.config)
  }

  multiGoldenTest("parse 3 test", "example3.%s") { config =>
    cycle("example3.yaml", config.golden, VocabularyYamlHint, target = Amf, config.config)
  }

  multiGoldenTest("parse 4 test", "example4.%s") { config =>
    cycle("example4.yaml", config.golden, VocabularyYamlHint, target = Amf, config.config)
  }

  multiGoldenTest("parse 5 test", "example5.%s") { config =>
    cycle("example5.yaml", config.golden, VocabularyYamlHint, target = Amf, config.config)
  }

  multiGoldenTest("parse 6 test", "example6.%s") { config =>
    cycle("example6.yaml", config.golden, VocabularyYamlHint, target = Amf, config.config)
  }

  multiGoldenTest("parse 7 test", "example7.%s") { config =>
    cycle("example7.yaml", config.golden, VocabularyYamlHint, target = Amf, config.config)
  }

  multiGoldenTest("parse 8 test", "example8.%s") { config =>
    cycle("example8.yaml", config.golden, VocabularyYamlHint, target = Amf, config.config)
  }

  multiGoldenTest("parse 9 test", "example9.%s") { config =>
    cycle("example9.yaml", config.golden, VocabularyYamlHint, target = Amf, config.config)
  }

  multiSourceTest("generate 1 test", "example1.%s") { config =>
    cycle(config.source, "example1.yaml", AmfJsonHint, target = Aml, AMLConfiguration.predefined())
  }

  multiSourceTest("generate 2 test", "example2.%s") { config =>
    cycle(config.source, "example2.yaml", AmfJsonHint, target = Aml, AMLConfiguration.predefined())
  }

  multiSourceTest("generate 3 test", "example3.%s") { config =>
    cycle(config.source, "example3.yaml", AmfJsonHint, target = Aml, AMLConfiguration.predefined())
  }

  multiSourceTest("generate 4 test", "example4.%s") { config =>
    cycle(config.source, "example4.yaml", AmfJsonHint, target = Aml, AMLConfiguration.predefined())
  }

  multiSourceTest("generate 5 test", "example5.%s") { config =>
    cycle(config.source, "example5.yaml", AmfJsonHint, target = Aml, AMLConfiguration.predefined())
  }

  multiSourceTest("generate 6 test", "example6.%s") { config =>
    cycle(config.source, "example6.yaml", AmfJsonHint, target = Aml, AMLConfiguration.predefined())
  }

  multiSourceTest("generate 7 test", "example7.%s") { config =>
    cycle(config.source, "example7.yaml", AmfJsonHint, target = Aml, AMLConfiguration.predefined())
  }

  multiGoldenTest("Multiple extends test", "vocabulary.%s") { config =>
    cycle("vocabulary.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          config.config,
          directory = s"${basePath}multiple-extends/")
  }
}
