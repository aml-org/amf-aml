package amf.client.model.domain

import amf.client.convert.VocabulariesClientConverter._
import amf.client.model.StrField
import amf.plugins.document.vocabularies.model.domain.{ExtensionMapping => InternalExtensionMapping}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class ExtensionMapping(override private[amf] val _internal: InternalExtensionMapping) extends DomainElement {

  @JSExportTopLevel("model.domain.ExtensionMapping")
  def this() = this(InternalExtensionMapping())

  def extensionName(): StrField = _internal.extensionName()

  def extensionMappingDefinition(): AnnotationMapping = _internal.extensionMappingDefinition()

  def withExtensionName(name: String): ExtensionMapping = {
    _internal.withExtensionName(name)
    this
  }

  def withExtensionMappingDefinition(annotationMapping: AnnotationMapping): ExtensionMapping = {
    _internal.withExtensionMappingDefinition(annotationMapping)
    this
  }
}
