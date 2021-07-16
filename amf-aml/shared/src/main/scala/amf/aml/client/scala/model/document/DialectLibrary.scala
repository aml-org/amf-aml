package amf.aml.client.scala.model.document

import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.document.DialectLibraryModel
import amf.aml.internal.metamodel.document.DialectModel.{Declares, References}

case class DialectLibrary(fields: Fields, annotations: Annotations)
    extends BaseUnit
    with ExternalContext[DialectLibrary]
    with DeclaresModel
    with MappingDeclarer {

  def references: Seq[BaseUnit]    = fields.field(References)
  def declares: Seq[DomainElement] = fields.field(Declares)

  private[amf] override def componentId: String = ""

  def meta: DialectLibraryModel.type = DialectLibraryModel
}

object DialectLibrary {
  def apply(): DialectLibrary = apply(Annotations())

  def apply(annotations: Annotations): DialectLibrary = DialectLibrary(Fields(), annotations)
}
