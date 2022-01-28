package amf.rdf.client.scala

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.document.ParsedDocument
import amf.core.internal.metamodel.Type
import amf.core.internal.plugins.document.graph.emitter.{
  SemanticExtensionAwareFieldRenderProvision,
  SemanticExtensionAwareMetaFieldRenderProvider
}
import amf.rdf.internal.RdfModelEmitter
import org.mulesoft.common.io.Output

case class RdfModelDocument(model: RdfModel) extends ParsedDocument

trait RdfFramework {

  def emptyRdfModel(): RdfModel

  def unitToRdfModel(unit: BaseUnit, config: AMFGraphConfiguration, options: RenderOptions): RdfModel = {
    val model      = emptyRdfModel()
    val extensions = config.registry.getEntitiesRegistry.extensionTypes
    new RdfModelEmitter(model, SemanticExtensionAwareMetaFieldRenderProvider(extensions, options)).emit(unit, options)
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
