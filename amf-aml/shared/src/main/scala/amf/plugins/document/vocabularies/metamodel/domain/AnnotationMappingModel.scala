package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.Iri
import amf.core.metamodel.domain.{DomainElementModel, ExternalModelVocabularies, ModelDoc, ModelVocabularies}
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.AnnotationMapping

object AnnotationMappingModel extends DomainElementModel with PropertyLikeMappingModel with NodeMappableModel {

  override val Name: Field = NodeMappingModel.Name

  val Domain: Field = Field(
      Iri,
      Namespace.AmfAml + "domain",
      ModelDoc(
          ModelVocabularies.AmlDoc,
          "domain",
          "Domain node type IRI for which a specific annotation mapping can be applied. Similar rdfs:domain but at an instance level, rather than schema level."
      )
  )

  override def fields: List[Field] =
    NodePropertyMapping :: Name :: LiteralRange :: ObjectRange ::
      MinCount :: Pattern :: Minimum :: Maximum :: AllowMultiple :: Sorted :: Enum :: TypeDiscriminator ::
      Unique :: ExternallyLinkable :: TypeDiscriminatorName :: Domain :: DomainElementModel.fields

  override def modelInstance: AnnotationMapping = AnnotationMapping()

  override val `type`: List[ValueType] = Namespace.Meta + "NodeAnnotationMapping" :: DomainElementModel.`type`
}
