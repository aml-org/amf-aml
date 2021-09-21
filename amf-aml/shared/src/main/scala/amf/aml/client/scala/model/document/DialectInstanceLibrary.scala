package amf.aml.client.scala.model.document

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.document.DialectInstanceLibraryModel
import amf.aml.internal.metamodel.document.DialectInstanceModel.{Declares, DefinedBy, GraphDependencies, References}

case class DialectInstanceLibrary(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with DeclaresModel
    with ComposedInstancesSupport {
  override def meta: DialectInstanceLibraryModel.type = DialectInstanceLibraryModel

  def declares: Seq[DomainElement] = fields.field(Declares)

  private[amf] override def componentId: String = ""
}

object DialectInstanceLibrary {
  def apply(): DialectInstanceLibrary = apply(Annotations())
  def apply(annotations: Annotations): DialectInstanceLibrary =
    DialectInstanceLibrary(Fields(), annotations)
}
