package amf.plugins.document.vocabularies.model.domain

import amf.core.model.StrField
import amf.core.model.domain.DomainElement
import amf.plugins.document.vocabularies.metamodel.domain.PropertyLikeMappingModel

trait PropertyLikeMapping[M <: PropertyLikeMappingModel] extends DomainElement with HasObjectRange[M] {
  def name(): StrField                = fields.field(meta.Name)
  def nodePropertyMapping(): StrField = fields.field(meta.NodePropertyMapping)
  def literalRange(): StrField        = fields.field(meta.LiteralRange)

  def withName(name: String): this.type                      = set(meta.Name, name)
  def withNodePropertyMapping(propertyId: String): this.type = set(meta.NodePropertyMapping, propertyId)
  def withLiteralRange(range: String): this.type             = set(meta.LiteralRange, range)

  def meta: M
}
