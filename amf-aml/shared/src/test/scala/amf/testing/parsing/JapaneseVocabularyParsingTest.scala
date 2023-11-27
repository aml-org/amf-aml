package amf.testing.parsing

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.internal.remote.Mimes
import amf.testing.common.utils.DialectTests

class JapaneseVocabularyParsingTest extends DialectTests {

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/japanese/dialects/"

  multiGoldenTest("parse 1 test", "example1.%s") { config =>
    cycle("example1.yaml", config.golden, Some(Mimes.`application/ld+json`), config.config)
  }

  multiGoldenTest("parse 2 test", "example2.%s") { config =>
    cycle("example2.yaml", config.golden, Some(Mimes.`application/ld+json`), config.config)
  }

  multiGoldenTest("parse 3 test", "example3.%s") { config =>
    cycle("example3.yaml", config.golden, Some(Mimes.`application/ld+json`), config.config)
  }

  multiGoldenTest("parse 4 test", "example4.%s") { config =>
    cycle("example4.yaml", config.golden, Some(Mimes.`application/ld+json`), config.config)
  }

  multiGoldenTest("parse 5 test", "example5.%s") { config =>
    cycle("example5.yaml", config.golden, Some(Mimes.`application/ld+json`), config.config)
  }

  multiGoldenTest("parse 6 test", "example6.%s") { config =>
    cycle("example6.yaml", config.golden, Some(Mimes.`application/ld+json`), config.config)
  }

  multiGoldenTest("parse 7 test", "example7.%s") { config =>
    cycle("example7.yaml", config.golden, Some(Mimes.`application/ld+json`), config.config)
  }

  multiSourceTest("generate 1 test", "example1.%s") { config =>
    cycle(config.source, "example1.yaml", mediaType = Some(Mimes.`application/yaml`), AMLConfiguration.predefined())
  }

  multiSourceTest("generate 2 test", "example2.%s") { config =>
    cycle(config.source, "example2.yaml", mediaType = Some(Mimes.`application/yaml`), AMLConfiguration.predefined())
  }

  multiSourceTest("generate 3 test", "example3.%s") { config =>
    cycle(config.source, "example3.yaml", mediaType = Some(Mimes.`application/yaml`), AMLConfiguration.predefined())
  }

  multiSourceTest("generate 4 test", "example4.%s") { config =>
    cycle(config.source, "example4.yaml", mediaType = Some(Mimes.`application/yaml`), AMLConfiguration.predefined())
  }

  multiSourceTest("generate 5 test", "example5.%s") { config =>
    cycle(config.source, "example5.yaml", mediaType = Some(Mimes.`application/yaml`), AMLConfiguration.predefined())
  }

  multiSourceTest("generate 6 test", "example6.%s") { config =>
    cycle(config.source, "example6.yaml", mediaType = Some(Mimes.`application/yaml`), AMLConfiguration.predefined())
  }

  multiSourceTest("generate 7 test", "example7.%s") { config =>
    cycle(config.source, "example7.yaml", mediaType = Some(Mimes.`application/yaml`), AMLConfiguration.predefined())
  }

  override def defaultRenderOptions: RenderOptions = RenderOptions().withSourceMaps.withPrettyPrint
}
