package amf.aml.client.scala.model.document

import amf.core.client.scala.model.document.{BaseUnit, EncodesModel}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.document.DialectFragmentModel
import amf.aml.internal.metamodel.document.DialectModel.{Encodes, References}
import amf.aml.client.scala.model.domain.NodeMapping

case class DialectFragment(fields: Fields, annotations: Annotations)
    extends BaseUnit
    with EncodesModel
    with ExternalContext[DialectFragment] {

  def references: Seq[BaseUnit]     = fields.field(References)
  override def encodes: NodeMapping = fields.field(Encodes)

  override def componentId: String = ""

  def withEncodes(nodeMapping: NodeMapping): DialectFragment = set(Encodes, nodeMapping)

  def meta: DialectFragmentModel.type = DialectFragmentModel
}

object DialectFragment {
  def apply(): DialectFragment = apply(Annotations())

  def apply(annotations: Annotations): DialectFragment = DialectFragment(Fields(), annotations)
}
