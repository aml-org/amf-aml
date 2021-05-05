package amf.plugins.document.vocabularies.metamodel.document

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Array, Iri}
import amf.core.metamodel.document._
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.document.DialectInstance

object DialectInstanceModel extends DocumentModel with ExternalContextModel {

  val DefinedBy: Field = Field(Iri, Namespace.Meta + "definedBy")

  val GraphDependencies: Field = Field(Array(Iri), Namespace.Document + "graphDependencies")

  override def modelInstance: AmfObject = DialectInstance()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstance" :: DocumentModel.`type`

  override val fields: List[Field] = DefinedBy :: GraphDependencies :: Externals :: DocumentModel.fields
}
