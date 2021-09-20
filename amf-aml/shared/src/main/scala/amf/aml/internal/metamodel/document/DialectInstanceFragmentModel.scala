package amf.aml.internal.metamodel.document

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri, Str}
import amf.core.internal.metamodel.document.{DocumentModel, FragmentModel}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.document.DialectInstanceFragment
import com.github.ghik.silencer.silent

object DialectInstanceFragmentModel extends DocumentModel with ExternalContextModel with DialectInstanceUnitModel {

  val Fragment: Field = Field(Str, Namespace.Meta + "fragment")

  override def modelInstance: AmfObject = DialectInstanceFragment()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstanceFragment" :: FragmentModel.`type`

  @silent("deprecated")
  override val fields: List[Field] = DefinedBy :: Fragment :: GraphDependencies :: Externals :: FragmentModel.fields
}
