package amf.client.model.domain

import amf.client.convert.VocabulariesClientConverter._
import amf.client.model.StrField
import amf.plugins.document.vocabularies.model.domain.{SemanticExtension => InternalSemanticExtension}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class SemanticExtension(override private[amf] val _internal: InternalSemanticExtension) extends DomainElement {

  @JSExportTopLevel("model.domain.SemanticExtension")
  def this() = this(InternalSemanticExtension())

  def extensionName(): StrField = _internal.extensionName()

  def extensionMappingDefinition(): StrField = _internal.extensionMappingDefinition()

  def withExtensionName(name: String): SemanticExtension = {
    _internal.withExtensionName(name)
    this
  }

  def withExtensionMappingDefinition(annotationMapping: String): SemanticExtension = {
    _internal.withExtensionMappingDefinition(annotationMapping)
    this
  }
}
