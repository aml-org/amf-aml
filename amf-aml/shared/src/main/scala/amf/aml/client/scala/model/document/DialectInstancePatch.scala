package amf.aml.client.scala.model.document

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.document.DialectInstanceModel._
import amf.aml.internal.metamodel.document.DialectInstancePatchModel

case class DialectInstancePatch(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with DeclaresModel
    with EncodesModel {

  override def meta: DialectInstancePatchModel.type = DialectInstancePatchModel

  def references: Seq[BaseUnit]        = fields.field(References)
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)
  def declares: Seq[DomainElement]     = fields.field(Declares)
  def definedBy(): StrField            = fields.field(DefinedBy)
  def extendsModel: StrField           = fields.field(DialectInstancePatchModel.Extends)
  override def encodes: DomainElement  = fields.field(Encodes)

  private[amf] override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstancePatch =
    set(DefinedBy, dialectId)
  def withGraphDependencies(ids: Seq[String]): DialectInstancePatch =
    set(GraphDependencies, ids)
  def withExtendsModel(target: String): DialectInstancePatch =
    set(DialectInstancePatchModel.Extends, target)
}

object DialectInstancePatch {
  def apply(): DialectInstancePatch = apply(Annotations())
  def apply(annotations: Annotations): DialectInstancePatch =
    DialectInstancePatch(Fields(), annotations)
}
