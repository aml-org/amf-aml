package amf.aml.client.scala.model.document

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.document.DialectInstanceLibraryModel
import amf.aml.internal.metamodel.document.DialectInstanceModel.{
  Declares,
  DefinedBy,
  GraphDependencies,
  References
}

case class DialectInstanceLibrary(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with DeclaresModel
    with ComposedInstancesSupport {
  override def meta: DialectInstanceLibraryModel.type = DialectInstanceLibraryModel

  def references: Seq[BaseUnit]        = fields.field(References)
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)
  def declares: Seq[DomainElement]     = fields.field(Declares)
  def definedBy(): StrField            = fields.field(DefinedBy)

  override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstanceLibrary =
    set(DefinedBy, dialectId)
  def withGraphDependencies(ids: Seq[String]): DialectInstanceLibrary =
    set(GraphDependencies, ids)
}

object DialectInstanceLibrary {
  def apply(): DialectInstanceLibrary = apply(Annotations())
  def apply(annotations: Annotations): DialectInstanceLibrary =
    DialectInstanceLibrary(Fields(), annotations)
}
