package amf.plugins.document.vocabularies.metamodel.document

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Array, Iri}
import amf.core.metamodel.document.{DocumentModel, ModuleModel}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.document.DialectInstanceLibrary

object DialectInstanceLibraryModel extends DocumentModel with ExternalContextModel {

  val DefinedBy         = Field(Iri, Namespace.Meta + "definedBy")
  val GraphDependencies = Field(Array(Iri), Namespace.Document + "graphDependencies")

  override def modelInstance: AmfObject = DialectInstanceLibrary()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstanceLibrary" :: ModuleModel.`type`

  override val fields: List[Field] = DefinedBy :: GraphDependencies :: Externals :: ModuleModel.fields
}
