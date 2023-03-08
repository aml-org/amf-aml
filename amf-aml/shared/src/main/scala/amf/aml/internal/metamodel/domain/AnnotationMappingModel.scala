package amf.aml.internal.metamodel.domain

import amf.core.internal.metamodel.{Field, Type}
import amf.core.internal.metamodel.Type.Iri
import amf.core.internal.metamodel.domain.{
  DomainElementModel,
  ExternalModelVocabularies,
  ModelDoc,
  ModelVocabularies,
  ShapeModel
}
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.domain.AnnotationMapping

object AnnotationMappingModel extends DomainElementModel with PropertyLikeMappingModel with NodeMappableModel {

  override val Name: Field = NodeMappingModel.Name

  val Domain: Field = Field(
    Type.Array(Iri),
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
