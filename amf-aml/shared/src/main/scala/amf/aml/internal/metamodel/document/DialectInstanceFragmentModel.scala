package amf.aml.internal.metamodel.document

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri, Str}
import amf.core.internal.metamodel.document.{DocumentModel, FragmentModel}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.document.DialectInstanceFragment

object DialectInstanceFragmentModel extends DocumentModel with ExternalContextModel {

  val DefinedBy: Field = Field(Iri, Namespace.Meta + "definedBy")

  val Fragment: Field = Field(Str, Namespace.Meta + "fragment")

  val GraphDependencies: Field = Field(Array(Iri), Namespace.Document + "graphDependencies")

  override def modelInstance: AmfObject = DialectInstanceFragment()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstanceFragment" :: FragmentModel.`type`

  override val fields: List[Field] = DefinedBy :: Fragment :: GraphDependencies :: Externals :: FragmentModel.fields
}
