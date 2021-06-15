package amf.aml.internal.metamodel.document

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri}
import amf.core.internal.metamodel.document._
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.document.DialectInstance

object DialectInstanceModel extends DocumentModel with ExternalContextModel {

  val DefinedBy: Field = Field(Iri, Namespace.Meta + "definedBy")

  val GraphDependencies: Field = Field(Array(Iri), Namespace.Document + "graphDependencies")

  override def modelInstance: AmfObject = DialectInstance()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstance" :: DocumentModel.`type`

  override val fields: List[Field] = DefinedBy :: GraphDependencies :: Externals :: DocumentModel.fields
}
