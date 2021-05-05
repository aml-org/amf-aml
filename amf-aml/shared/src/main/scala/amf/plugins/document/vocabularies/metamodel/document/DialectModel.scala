package amf.plugins.document.vocabularies.metamodel.document

import amf.core.metamodel.Field
import amf.core.metamodel.Type.Str
import amf.core.metamodel.document.{BaseUnitModel, DocumentModel}
import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.metamodel.domain.{DocumentsModelModel, ExtensionMappingModel}
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.core.metamodel.Type.Array

object DialectModel extends DocumentModel with ExternalContextModel {

  val Name: Field =
    Field(Str, Namespace.Core + "name", ModelDoc(ModelVocabularies.Core, "name", "Name of the dialect"))

  val Version: Field =
    Field(Str, Namespace.Core + "version", ModelDoc(ModelVocabularies.Core, "version", "Version of the dialect"))

  val Documents: Field = Field(DocumentsModelModel,
                               Namespace.Meta + "documents",
                               ModelDoc(ModelVocabularies.Meta, "documents", "Document mapping for the the dialect"))

  val Extensions: Field = Field(
      Array(ExtensionMappingModel),
      Namespace.Meta + "extensions",
      ModelDoc(ModelVocabularies.Meta,
               "extensions",
               "Extensions mappings derived from annotation mappings declarations in a dialect")
  )

  override def modelInstance: AmfObject = Dialect()

  override val `type`: List[ValueType] = Namespace.Meta + "Dialect" :: DocumentModel.`type`

  override val fields
    : List[Field] = Name :: Version :: Externals :: Documents :: BaseUnitModel.Location :: DocumentModel.fields

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Meta,
      "Dialect",
      "Definition of an AML dialect, mapping AST nodes from dialect documents into an output semantic graph"
  )
}
