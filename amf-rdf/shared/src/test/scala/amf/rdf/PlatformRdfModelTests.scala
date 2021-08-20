package amf.rdf

import amf.core.internal.convert.NativeOps
import amf.rdf.client.platform.RdfModel
import amf.rdf.internal.unsafe.RdfPlatformSecrets
import org.scalatest.FunSuite

trait PlatformRdfModelTests extends FunSuite with RdfPlatformSecrets with NativeOps {

  test("Basic triple manipulation") {
    val model   = RdfModel.empty()
    val subject = "http://test.com/a"
    model.addLiteralTriple(subject, "http://test.com/p", "test")
    val resBefore = model.findNode(subject)
    assert(resBefore.asOption.isDefined)
    assert(resBefore.asOption.get.getKeys().asSeq.size == 1)
    model.addLiteralTriple(subject, "http://test.com/pp", "test")
    val resAfter = model.findNode(subject)
    assert(resAfter.asOption.isDefined)
    assert(resAfter.asOption.get.getKeys().asSeq.size == 2)
  }
}
