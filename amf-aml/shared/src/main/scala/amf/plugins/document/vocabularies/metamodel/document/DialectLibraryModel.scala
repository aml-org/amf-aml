package amf.plugins.document.vocabularies.metamodel.document

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.document.{DocumentModel, ModuleModel}
import amf.core.internal.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.document.DialectLibrary

object DialectLibraryModel extends ModuleModel with ExternalContextModel {
  override def modelInstance: AmfObject = DialectLibrary()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectLibrary" :: DocumentModel.`type`

  override val fields: List[Field] = Externals :: Location :: ModuleModel.fields

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Meta,
      "DialectLibrary",
      "Library of AML mappings that can be reused in different AML dialects"
  )
}
