package amf.testing.unclassified

import amf.core.client.scala.model.domain.DomainElement
import amf.testing.common.utils.DialectTests
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

trait IteratorTest extends DialectTests with Matchers {

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/instances/"
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Domain element iterator in self encoded dialect instance") {
    parseInstance("dialect23.yaml", "example23.yaml").map(baseUnit => {
      val ids =
        baseUnit.iterator().collect { case e: DomainElement => e.id }.toStream
      ids should contain inOrderOnly
        ("file://amf-aml/shared/src/test/resources/vocabularies2/instances/example23.yaml",
        "file://amf-aml/shared/src/test/resources/vocabularies2/instances/example23.yaml#/lets")
    })
  }

  test("findById in self encoded dialect instance") {
    parseInstance("dialect23.yaml", "example23.yaml").map(baseUnit => {
      assert(
          baseUnit
            .findById("file://amf-aml/shared/src/test/resources/vocabularies2/instances/example23.yaml#/lets")
            .isDefined
      )
    })
  }

}
