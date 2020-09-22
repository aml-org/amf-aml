package amf.plugins.document.vocabularies.model.document

import amf.core.metamodel.Obj
import amf.core.model.StrField
import amf.core.model.document.{BaseUnit, EncodesModel}
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceFragmentModel
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceFragmentModel.Fragment
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceModel.{
  DefinedBy,
  Encodes,
  GraphDependencies,
  References
}

case class DialectInstanceFragment(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with EncodesModel
    with ComposedInstancesSupport {
  override def meta: Obj = DialectInstanceFragmentModel

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
