package amf.aml.client.scala.model.document

import amf.core.client.scala.model.domain.AmfObject
import amf.aml.internal.metamodel.document.ExternalContextModelFields
import amf.aml.client.scala.model.domain.External

trait ExternalContext[T <: AmfObject] { this: T =>
  def externals: Seq[External] = fields.field(ExternalContextModelFields.Externals)
  def withExternals(externals: Seq[External]): T =
    setArray(ExternalContextModelFields.Externals, externals).asInstanceOf[T]
}
