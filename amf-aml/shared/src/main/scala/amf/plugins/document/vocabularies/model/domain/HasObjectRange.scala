package amf.plugins.document.vocabularies.model.domain

import amf.core.model.StrField
import amf.core.model.domain.DomainElement
import amf.plugins.document.vocabularies.metamodel.domain.HasObjectRangeModel

trait HasObjectRange[M <: HasObjectRangeModel] extends DomainElement {
  def objectRange(): Seq[StrField]                   = fields.field(meta.ObjectRange)
  def withObjectRange(range: Seq[String]): this.type = set(meta.ObjectRange, range)

  override def meta: M
}
