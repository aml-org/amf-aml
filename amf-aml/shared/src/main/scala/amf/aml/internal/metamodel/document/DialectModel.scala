package amf.aml.internal.metamodel.document

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Str
import amf.core.internal.metamodel.document.{BaseUnitModel, DocumentModel}
import amf.core.internal.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.internal.metamodel.domain.{DocumentsModelModel, SemanticExtensionModel}
import amf.aml.client.scala.model.document.Dialect
import amf.core.internal.metamodel.Type.Array

object DialectModel extends DocumentModel with ExternalContextModel {

  val Name: Field =
    Field(Str, Namespace.Core + "name", ModelDoc(ModelVocabularies.Core, "name", "Name of the dialect"))

  val Version: Field =
    Field(Str, Namespace.Core + "version", ModelDoc(ModelVocabularies.Core, "version", "Version of the dialect"))

  val Documents: Field = Field(
    DocumentsModelModel,
    Namespace.Meta + "documents",
    ModelDoc(ModelVocabularies.Meta, "documents", "Document mapping for the the dialect")
  )

  val Extensions: Field = Field(
    Array(SemanticExtensionModel),
    Namespace.Meta + "extensions",
    ModelDoc(
      ModelVocabularies.Meta,
      "extensions",
      "Extensions mappings derived from annotation mappings declarations in a dialect"
    )
  )

  override def modelInstance: AmfObject = Dialect()

  override val `type`: List[ValueType] = Namespace.Meta + "Dialect" :: DocumentModel.`type`

  override val fields: List[Field] =
    Name :: Version :: Extensions :: Externals :: Documents :: BaseUnitModel.Location :: DocumentModel.fields

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.Meta,
    "Dialect",
    "Definition of an AML dialect, mapping AST nodes from dialect documents into an output semantic graph"
  )
}
