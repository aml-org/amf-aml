package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Iri, Array}
import amf.core.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.AnnotationMapping

object AnnotationMappingModel extends DomainElementModel with MergeableMappingModel with NodeWithDiscriminatorModel {


  val AnnotationTarget: Field = Field(
    Array(Iri),
    Namespace.Meta + "annotationTarget",
    ModelDoc(ModelVocabularies.Meta,
      "annotationTarget",
      "Target class whose instances can be extended with this annotation")
  )

  override val `type`: List[ValueType] = Namespace.Meta + "AnnotationMapping" :: DomainElementModel.`type`

  override def modelInstance: AmfObject = AnnotationMapping()

  override def fields: List[Field] = AnnotationTarget :: PropertyMappingModel.fields

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.Meta,
    "AnnotationMapping",
    "Semantic mapping for an AST node used to extend other documents"
  )
}
