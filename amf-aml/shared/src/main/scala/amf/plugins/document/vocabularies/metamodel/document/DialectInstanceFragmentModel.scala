package amf.plugins.document.vocabularies.metamodel.document

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Array, Iri, Str}
import amf.core.metamodel.document.{DocumentModel, FragmentModel}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.document.DialectInstanceFragment

object DialectInstanceFragmentModel extends DocumentModel with ExternalContextModel {

  val DefinedBy: Field = Field(Iri, Namespace.Meta + "definedBy")

  val Fragment: Field = Field(Str, Namespace.Meta + "fragment")

  val GraphDependencies: Field = Field(Array(Iri), Namespace.Document + "graphDependencies")

  override def modelInstance: AmfObject = DialectInstanceFragment()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstanceFragment" :: FragmentModel.`type`

  override val fields: List[Field] = DefinedBy :: Fragment :: GraphDependencies :: Externals :: FragmentModel.fields
}
