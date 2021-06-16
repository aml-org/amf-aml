package amf.aml.client.scala.model.domain

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.DomainElement
import amf.aml.internal.metamodel.domain.HasObjectRangeModel

trait HasObjectRange[M <: HasObjectRangeModel] extends DomainElement {
  def objectRange(): Seq[StrField]                   = fields.field(meta.ObjectRange)
  def withObjectRange(range: Seq[String]): this.type = set(meta.ObjectRange, range)

  override def meta: M
}
