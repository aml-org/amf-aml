package amf.rdf.client.platform

import amf.rdf.internal.convert.RdfClientConverter._
import amf.rdf.client.scala.{RdfModel => InternalRdfModel}
import amf.rdf.internal.unsafe.RdfPlatformSecrets

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("RdfModel")
object RdfModel extends RdfPlatformSecrets {
  def empty(): RdfModel = new RdfModel(framework.emptyRdfModel())
}

@JSExportAll
class RdfModel(private[amf] var internal: InternalRdfModel) {

  def addTriple(subject: String, predicate: String, objResource: String): RdfModel = {
    this.internal = internal.addTriple(subject, predicate, objResource)
    this
  }

  def addLiteralTriple(subject: String, predicate: String, objLiteralValue: String): RdfModel = {
    this.internal = internal.addTriple(subject, predicate, objLiteralValue, None)
    this
  }

  def addLiteralTriple(subject: String, predicate: String, objLiteralValue: String, objLiteralType: String): RdfModel = {
    this.internal = internal.addTriple(subject, predicate, objLiteralValue, Some(objLiteralType))
    this
  }

  def findNode(uri: String): ClientOption[Node] = internal.findNode(uri).asClient

  def nextAnonId(): String = internal.nextAnonId()

  /**
    * Load RDF string representation in this model
    * @param text
    * @param mediaType
    */
  def load(mediaType: String, text: String) = internal.load(mediaType, text)

  /**
    * Write model as a String representation
    * @param mediaType
    * @return
    */
  def serializeString(mediaType: String): ClientOption[String] = internal.serializeString(mediaType).asClient

  def toN3(): String = internal.toN3()

  def native(): Any = internal.native()
}
