package amf.rdf

import amf.rdf.internal.unsafe.RdfPlatformSecrets
import org.scalatest.funsuite.AnyFunSuite

class RdfModelTests extends AnyFunSuite with RdfPlatformSecrets {

  test("Manipulation of triples should invalidate cache") {
    val model   = framework.emptyRdfModel()
    val subject = "http://test.com/a"
    model.addTriple(subject, "http://test.com/p", "test", None)
    val resBefore = model.findNode(subject)
    assert(resBefore.isDefined)
    assert(resBefore.get.getKeys().size == 1)
    model.addTriple(subject, "http://test.com/pp", "test", None)
    val resAfter = model.findNode(subject)
    assert(resAfter.isDefined)
    assert(resAfter.get.getKeys().size == 2)
  }
}
