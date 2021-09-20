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

  def declares: Seq[DomainElement]    = fields.field(Declares)
  def extendsModel: StrField          = fields.field(DialectInstancePatchModel.Extends)
  override def encodes: DomainElement = fields.field(Encodes)

  private[amf] override def componentId: String = ""

  def withExtendsModel(target: String): DialectInstancePatch =
    set(DialectInstancePatchModel.Extends, target)
}

object DialectInstancePatch {
  def apply(): DialectInstancePatch = apply(Annotations())
  def apply(annotations: Annotations): DialectInstancePatch =
    DialectInstancePatch(Fields(), annotations)
}
