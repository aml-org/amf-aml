package amf.plugins.document.vocabularies.model.document

import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.document.DialectLibraryModel
import amf.plugins.document.vocabularies.metamodel.document.DialectModel.{Declares, References}

case class DialectLibrary(fields: Fields, annotations: Annotations)
    extends BaseUnit
    with ExternalContext[DialectLibrary]
    with DeclaresModel
    with MappingDeclarer {

  def references: Seq[BaseUnit]    = fields.field(References)
  def declares: Seq[DomainElement] = fields.field(Declares)

  override def componentId: String = ""

  def meta: DialectLibraryModel.type = DialectLibraryModel
}

object DialectLibrary {
  def apply(): DialectLibrary = apply(Annotations())

  def apply(annotations: Annotations): DialectLibrary = DialectLibrary(Fields(), annotations)
}
