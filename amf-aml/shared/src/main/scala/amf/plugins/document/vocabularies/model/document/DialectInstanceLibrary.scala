package amf.plugins.document.vocabularies.model.document

import amf.core.model.StrField
import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceLibraryModel
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceModel.{Declares, DefinedBy, References}

case class DialectInstanceLibrary(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with DeclaresModel
    with ComposedInstancesSupport {
  override def meta: DialectInstanceLibraryModel.type = DialectInstanceLibraryModel

  def references: Seq[BaseUnit]        = fields.field(References)
  def graphDependencies: Seq[StrField] = amfProcessingData.graphDependencies
  def declares: Seq[DomainElement]     = fields.field(Declares)
  def definedBy(): StrField            = fields.field(DefinedBy)

  override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstanceLibrary =
    set(DefinedBy, dialectId)
}

object DialectInstanceLibrary {
  def apply(): DialectInstanceLibrary = apply(Annotations())
  def apply(annotations: Annotations): DialectInstanceLibrary =
    DialectInstanceLibrary(Fields(), annotations)
}
