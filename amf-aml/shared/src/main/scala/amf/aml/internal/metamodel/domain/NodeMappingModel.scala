package amf.aml.internal.metamodel.domain

import amf.aml.client.scala.model.domain.NodeMapping
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri, Str}
import amf.core.internal.metamodel.domain._

object NodeMappingModel extends AnyMappingModel with ClosedModel {

  val NodeTypeMapping: Field = Field(
      Iri,
      Namespace.Shacl + "targetClass",
      ModelDoc(
          ExternalModelVocabularies.Shacl,
          "targetClass",
          "Target class whose instances will need to match the constraint described for the node"
      )
  )
  val PropertiesMapping: Field = Field(
      Array(PropertyMappingModel),
      Namespace.Shacl + "property",
      ModelDoc(ExternalModelVocabularies.Shacl, "property", "Data shape constraint for a property of the target node")
  )
  val IdTemplate: Field = Field(
      Str,
      Namespace.ApiContract + "uriTemplate",
      ModelDoc(
          ModelVocabularies.ApiContract,
          "uriTemplate",
          "URI template that will be used to generate the URI of the parsed nodeds in the graph"
      )
  )
  val ResolvedExtends: Field = Field(Array(Iri), Namespace.Meta + "resolvedExtends")

  override def fields: List[Field] =
    NodeTypeMapping :: Name :: PropertiesMapping :: IdTemplate :: MergePolicy :: ResolvedExtends :: Closed :: LinkableElementModel.fields ++ DomainElementModel.fields ++ AnyMappingModel.fields

  override def modelInstance: AmfObject = NodeMapping()

  override val `type`: List[ValueType] =
    Namespace.Meta + "NodeMapping" :: Namespace.Shacl + "Shape" :: DomainElementModel.`type`
}
