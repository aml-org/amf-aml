package amf.aml.internal.metamodel.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Bool, Str}
import amf.core.internal.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.domain.DocumentsModel

object DocumentsModelModel extends DomainElementModel {

  val Root: Field = Field(
    DocumentMappingModel,
    Namespace.Meta + "rootDocument",
    ModelDoc(ModelVocabularies.Meta, "rootDocument", "Root node encoded in a mapped document base unit")
  )

  val Fragments: Field = Field(
    Array(DocumentMappingModel),
    Namespace.Meta + "fragments",
    ModelDoc(ModelVocabularies.Meta, "fragments", "Mapping of fragment base unit for a particular dialect")
  )

  val Library: Field = Field(
    DocumentMappingModel,
    Namespace.Meta + "library",
    ModelDoc(ModelVocabularies.Meta, "library", "Mappig of module base unit for a particular dialect")
  )
  // options:

  val SelfEncoded: Field = Field(
    Bool,
    Namespace.Meta + "selfEncoded",
    ModelDoc(
      ModelVocabularies.Meta,
      "selfEncoded",
      "Information about if the base unit URL should be the same as the URI of the parsed root nodes in the unit"
    )
  )

  val DeclarationsPath: Field = Field(
    Str,
    Namespace.Meta + "declarationsPath",
    ModelDoc(
      ModelVocabularies.Meta,
      "declarationsPath",
      "Information about the AST location of the declarations to be parsed as declared domain elements"
    )
  )

  val KeyProperty: Field = Field(
    Bool,
    Namespace.Meta + "keyProperty",
    ModelDoc(
      ModelVocabularies.Meta,
      "keyProperty",
      "Information about whether the dialect is defined by the header or a key property"
    )
  )

  val ReferenceStyle: Field = Field(
    Str,
    Namespace.Meta + "referenceStyle",
    ModelDoc(
      ModelVocabularies.Meta,
      "referenceStyle",
      "Determines the style for inclusions (RamlStyle or JsonSchemaStyle)"
    )
  )

  override def fields: List[Field] =
    Root :: Fragments :: Library :: SelfEncoded :: DeclarationsPath :: KeyProperty :: ReferenceStyle :: DomainElementModel.fields

  override def modelInstance: AmfObject = DocumentsModel()

  override val `type`: List[ValueType] = Namespace.Meta + "DocumentsModel" :: DomainElementModel.`type`

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.Meta,
    "DocumentsModel",
    "Mapping from different type of dialect documents to base units in the parsed graph"
  )
}
