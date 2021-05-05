package amf.plugins.document.vocabularies.model.domain

import amf.core.model.StrField
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.domain.ExtensionMappingModel
import amf.plugins.document.vocabularies.metamodel.domain.ExtensionMappingModel._
import org.yaml.model.YMap

case class ExtensionMapping(fields: Fields, annotations: Annotations) extends DomainElement {
  override def meta: ExtensionMappingModel.type = ExtensionMappingModel

  def extensionName(): StrField                       = fields.field(ExtensionName)
  def extensionMappingDefinition(): AnnotationMapping = fields.field(ExtensionMappingDefinition)

  def withExtensionName(name: String): ExtensionMapping = set(ExtensionName, name)
  def withExtensionMappingDefinition(annotationMapping: AnnotationMapping): ExtensionMapping =
    set(ExtensionMappingDefinition, annotationMapping)

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = s"extensionMappings/${extensionName().value()}"
}

object ExtensionMapping {
  def apply(): ExtensionMapping                         = apply(Annotations())
  def apply(ast: YMap): ExtensionMapping                = apply(Annotations(ast))
  def apply(annotations: Annotations): ExtensionMapping = ExtensionMapping(Fields(), annotations)
}
