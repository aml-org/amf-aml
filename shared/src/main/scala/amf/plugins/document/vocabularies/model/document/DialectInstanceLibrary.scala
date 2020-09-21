package amf.plugins.document.vocabularies.model.document

import amf.core.metamodel.Obj
import amf.core.model.StrField
import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceLibraryModel
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceModel.{
  Declares,
  DefinedBy,
  GraphDependencies,
  References
}

case class DialectInstanceLibrary(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with DeclaresModel
    with ComposedInstancesSupport {
  override def meta: Obj = DialectInstanceLibraryModel

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
