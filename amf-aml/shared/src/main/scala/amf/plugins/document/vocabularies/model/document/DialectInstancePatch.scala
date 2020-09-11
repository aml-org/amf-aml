package amf.plugins.document.vocabularies.model.document

import amf.core.metamodel.Obj
import amf.core.model.StrField
import amf.core.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceModel._
import amf.plugins.document.vocabularies.metamodel.document.DialectInstancePatchModel

case class DialectInstancePatch(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with DeclaresModel
    with EncodesModel {

  override def meta: Obj = DialectInstancePatchModel

  def references: Seq[BaseUnit]        = fields.field(References)
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)
  def declares: Seq[DomainElement]     = fields.field(Declares)
  def definedBy(): StrField            = fields.field(DefinedBy)
  def extendsModel: StrField           = fields.field(DialectInstancePatchModel.Extends)
  override def encodes: DomainElement  = fields.field(Encodes)

  override def componentId: String = ""

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
