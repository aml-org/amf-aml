package amf.plugins.document.vocabularies.metamodel.document

import amf.core.metamodel.Field
import amf.core.metamodel.document.{DocumentModel, ModuleModel}
import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.document.DialectLibrary

object DialectLibraryModel extends ModuleModel with ExternalContextModel {
  override def modelInstance: AmfObject = DialectLibrary()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectLibrary" :: DocumentModel.`type`

  override val fields: List[Field] = Externals :: Location :: ModuleModel.fields

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Meta,
      "Dialect Library",
      "Library of AML mappings that can be reused in different AML dialects"
  )
}
