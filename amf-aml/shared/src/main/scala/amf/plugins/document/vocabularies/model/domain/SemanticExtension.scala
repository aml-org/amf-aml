package amf.plugins.document.vocabularies.model.domain

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.domain.SemanticExtensionModel
import amf.plugins.document.vocabularies.metamodel.domain.SemanticExtensionModel._
import org.yaml.model.YMap

case class SemanticExtension(fields: Fields, annotations: Annotations) extends DomainElement {
  override def meta: SemanticExtensionModel.type = SemanticExtensionModel

  def extensionName(): StrField              = fields.field(ExtensionName)
  def extensionMappingDefinition(): StrField = fields.field(ExtensionMappingDefinition)

  def withExtensionName(name: String): SemanticExtension = set(ExtensionName, name)
  def withExtensionMappingDefinition(annotationMapping: String): SemanticExtension =
    set(ExtensionMappingDefinition, annotationMapping)

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = s"extensionMappings/${extensionName().value()}"
}

object SemanticExtension {
  def apply(): SemanticExtension                         = apply(Annotations())
  def apply(ast: YMap): SemanticExtension                = apply(Annotations(ast))
  def apply(annotations: Annotations): SemanticExtension = SemanticExtension(Fields(), annotations)
}
