package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Array, Iri, Str}
import amf.core.metamodel.domain.{
  DomainElementModel,
  LinkableElementModel,
  ModelDoc,
  ModelVocabularies,
  ExternalModelVocabularies
}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.domain.NodeMapping

object NodeMappingModel
    extends DomainElementModel
    with LinkableElementModel
    with MergeableMappingModel
    with NodeMappableModel {

  val NodeTypeMapping = Field(
      Iri,
      Namespace.Shacl + "targetClass",
      ModelDoc(ExternalModelVocabularies.Shacl,
               "targetClass",
               "Target class whose instances will need to match the constraint described for the node")
  )
  val PropertiesMapping = Field(
      Array(PropertyMappingModel),
      Namespace.Shacl + "property",
      ModelDoc(ExternalModelVocabularies.Shacl, "property", "Data shape constraint for a property of the target node")
  )
  val IdTemplate = Field(
      Str,
      Namespace.ApiContract + "uriTemplate",
      ModelDoc(ModelVocabularies.ApiContract,
               "uriTemplate",
               "URI template that will be used to generate the URI of the parsed nodeds in the graph")
  )
  val ResolvedExtends = Field(Array(Iri), Namespace.Meta + "resolvedExtends")

  override def fields: List[Field] =
    NodeTypeMapping :: Name :: PropertiesMapping :: IdTemplate :: MergePolicy :: ResolvedExtends :: LinkableElementModel.fields ++ DomainElementModel.fields

  override def modelInstance: AmfObject = NodeMapping()

  override val `type`
    : List[ValueType] = Namespace.Meta + "NodeMapping" :: Namespace.Shacl + "Shape" :: DomainElementModel.`type`
}
