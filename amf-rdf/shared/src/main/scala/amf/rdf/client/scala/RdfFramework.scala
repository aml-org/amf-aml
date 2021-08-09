package amf.rdf.client.scala

import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.ParsedDocument
import amf.rdf.internal.RdfModelEmitter
import org.mulesoft.common.io.Output

case class RdfModelDocument(model: RdfModel) extends ParsedDocument

trait RdfFramework {

  def emptyRdfModel(): RdfModel

  def unitToRdfModel(unit: BaseUnit, options: RenderOptions): RdfModel = {
    val model = emptyRdfModel()
    new RdfModelEmitter(model).emit(unit, options)
    model
  }

  def syntaxToRdfModel(mediaType: String, text: CharSequence): RdfModelDocument = {
    val model = emptyRdfModel()
    model.load(mediaType, text.toString)
    RdfModelDocument(model)
  }

  def rdfModelToSyntax(mediaType: String, rdfModelDocument: RdfModelDocument): Option[String] = {
    rdfModelDocument.model.serializeString(mediaType)
  }

  def rdfModelToSyntaxWriter[W: Output](mediaType: String, rdfModelDocument: RdfModelDocument, writer: W): Option[W] = {
    rdfModelDocument.model.serializeWriter(mediaType, writer)
  }
}
