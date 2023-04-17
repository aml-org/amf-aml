package amf.aml.internal.metamodel.document

import amf.aml.client.scala.model.document.DialectInstanceFragment
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Str
import amf.core.internal.metamodel.document.{DocumentModel, FragmentModel}

import scala.annotation.nowarn

object DialectInstanceFragmentModel extends DocumentModel with ExternalContextModel with DialectInstanceUnitModel {

  val Fragment: Field = Field(Str, Namespace.Meta + "fragment")

  override def modelInstance: AmfObject = DialectInstanceFragment()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstanceFragment" :: FragmentModel.`type`

  @nowarn
  override val fields: List[Field] = DefinedBy :: Fragment :: GraphDependencies :: Externals :: FragmentModel.fields
}
