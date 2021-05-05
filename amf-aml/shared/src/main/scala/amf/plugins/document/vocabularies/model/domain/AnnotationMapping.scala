package amf.plugins.document.vocabularies.model.domain

import amf.core.model.StrField
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.domain.AnnotationMappingModel
import amf.plugins.document.vocabularies.metamodel.domain.AnnotationMappingModel._
import org.yaml.model.YMap

case class AnnotationMapping(fields: Fields, annotations: Annotations)
    extends PropertyLikeMapping[AnnotationMappingModel.type] {
  def target(): StrField                               = fields.field(Target)
  def withTarget(targetIri: String): AnnotationMapping = set(Target, targetIri)

  override def meta: AnnotationMappingModel.type = AnnotationMappingModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = s"annotation-mappings/${name().value()}"
}

object AnnotationMapping {
  def apply(): AnnotationMapping = apply(Annotations())

  def apply(ast: YMap): AnnotationMapping = apply(Annotations(ast))

  def apply(annotations: Annotations): AnnotationMapping =
    AnnotationMapping(Fields(), annotations)
}
