package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.Iri
import amf.core.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.AnnotationMapping

object AnnotationMappingModel extends DomainElementModel with PropertyLikeMappingModel {

  val Target: Field = Field(Iri,
                            Namespace.Document + "target",
                            ModelDoc(ModelVocabularies.AmlDoc,
                                     "target",
                                     "Target node IRI for which a specific annotation mapping can be applied"))

  override def fields: List[Field] =
    Name :: LiteralRange :: ObjectRange :: NodePropertyMapping :: Target :: DomainElementModel.fields

  override def modelInstance: AnnotationMapping = AnnotationMapping()

  override val `type`: List[ValueType] = Namespace.Meta + "NodeAnnotationMapping" :: DomainElementModel.`type`
}
