package amf.aml.client.scala.model.document

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.document.{BaseUnit, EncodesModel}
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.document.DialectInstanceFragmentModel
import amf.aml.internal.metamodel.document.DialectInstanceFragmentModel.Fragment
import amf.aml.internal.metamodel.document.DialectInstanceModel.{
  DefinedBy,
  Encodes,
  GraphDependencies,
  References
}

case class DialectInstanceFragment(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with EncodesModel
    with ComposedInstancesSupport {
  override def meta: DialectInstanceFragmentModel.type = DialectInstanceFragmentModel

  def references: Seq[BaseUnit]        = fields.field(References)
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)
  def encodes: DomainElement           = fields.field(Encodes)
  def definedBy(): StrField            = fields.field(DefinedBy)
  def fragment(): StrField             = fields.field(Fragment)

  override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstanceFragment =
    set(DefinedBy, dialectId)
  def withFragment(fragmentId: String): DialectInstanceFragment =
    set(Fragment, fragmentId)
  def withGraphDepencies(ids: Seq[String]): DialectInstanceFragment =
    set(GraphDependencies, ids)
}

object DialectInstanceFragment {
  def apply(): DialectInstanceFragment = apply(Annotations())
  def apply(annotations: Annotations): DialectInstanceFragment =
    DialectInstanceFragment(Fields(), annotations)
}
