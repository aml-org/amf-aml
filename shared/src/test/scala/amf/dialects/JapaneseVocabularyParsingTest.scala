package amf.dialects

import amf.core.remote.{Amf, AmfJsonHint, Aml, VocabularyYamlHint}
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class JapaneseVocabularyParsingTest extends DialectTests{
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "shared/src/test/resources/vocabularies2/japanese/dialects/"

  test("parse 1 test") {
    init().flatMap(_ => cycle("example1.raml", "example1.json", VocabularyYamlHint, Amf))
  }

  test("parse 2 test") {
    init().flatMap(_ => cycle("example2.raml", "example2.json", VocabularyYamlHint, Amf))
  }

  test("parse 3 test") {
    init().flatMap(_ => cycle("example3.raml", "example3.json", VocabularyYamlHint, Amf))
  }

  test("parse 4 test") {
    init().flatMap(_ => cycle("example4.raml", "example4.json", VocabularyYamlHint, Amf))
  }

  test("parse 5 test") {
    init().flatMap(_ => cycle("example5.raml", "example5.json", VocabularyYamlHint, Amf))
  }

  test("parse 6 test") {
    init().flatMap(_ => cycle("example6.raml", "example6.json", VocabularyYamlHint, Amf))
  }

  test("parse 7 test") {
    init().flatMap(_ => cycle("example7.raml", "example7.json", VocabularyYamlHint, Amf))
  }

  test("generate 1 test") {
    init().flatMap(_ => cycle("example1.json", "example1.raml", AmfJsonHint, Aml))
  }

  test("generate 2 test") {
    init().flatMap(_ => cycle("example2.json", "example2.raml", AmfJsonHint, Aml))
  }

  test("generate 3 test") {
    init().flatMap(_ => cycle("example3.json", "example3.raml", AmfJsonHint, Aml))
  }

  test("generate 4 test") {
    init().flatMap(_ => cycle("example4.json", "example4.raml", AmfJsonHint, Aml))
  }

  test("generate 5 test") {
    init().flatMap(_ => cycle("example5.json", "example5.raml", AmfJsonHint, Aml))
  }

  test("generate 6 test") {
    init().flatMap(_ => cycle("example6.json", "example6.raml", AmfJsonHint, Aml))
  }

  test("generate 7 test") {
    init().flatMap(_ => cycle("example7.json", "example7.raml", AmfJsonHint, Aml))
  }
}
