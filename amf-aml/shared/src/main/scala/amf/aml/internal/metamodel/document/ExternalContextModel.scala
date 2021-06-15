package amf.aml.internal.metamodel.document

import amf.core.internal.metamodel.Type.Array
import amf.core.internal.metamodel.{Field, Obj}
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.internal.metamodel.domain.ExternalModel

trait ExternalContextModel extends Obj {
  val Externals: Field = Field(Array(ExternalModel), Namespace.Meta + "externals")
}

object ExternalContextModelFields extends ExternalContextModel {
  override val fields: List[Field] = Externals :: Nil

  override val `type`: List[ValueType] = Nil
}
