package amf.plugins.document.vocabularies.model.domain

import amf.core.client.scala.model.{AnyField, BoolField, DoubleField, IntField, StrField}
import amf.core.client.scala.model.domain.{AmfScalar, DomainElement}
import amf.plugins.document.vocabularies.metamodel.domain.PropertyLikeMappingModel

trait PropertyLikeMapping[M <: PropertyLikeMappingModel]
    extends DomainElement
    with HasObjectRange[M]
    with NodeWithDiscriminator[M] {

  def name(): StrField                = fields.field(meta.Name)
  def nodePropertyMapping(): StrField = fields.field(meta.NodePropertyMapping)
  def literalRange(): StrField        = fields.field(meta.LiteralRange)
  def minCount(): IntField            = fields.field(meta.MinCount)
  def pattern(): StrField             = fields.field(meta.Pattern)
  def minimum(): DoubleField          = fields.field(meta.Minimum)
  def maximum(): DoubleField          = fields.field(meta.Maximum)
  def allowMultiple(): BoolField      = fields.field(meta.AllowMultiple)
  def sorted(): BoolField             = fields.field(meta.Sorted)
  def enum(): Seq[AnyField]           = fields.field(meta.Enum)
  def unique(): BoolField             = fields.field(meta.Unique)
  def externallyLinkable(): BoolField = fields.field(meta.ExternallyLinkable)
  def nodesInRange: Seq[String] = {
    val range = objectRange()
    if (range.isEmpty) {
      Option(typeDiscriminator()).getOrElse(Map()).values.toSeq
    } else {
      range.map(_.value())
    }
  }

  def withName(name: String): this.type                      = set(meta.Name, name)
  def withNodePropertyMapping(propertyId: String): this.type = set(meta.NodePropertyMapping, propertyId)
  def withLiteralRange(range: String): this.type             = set(meta.LiteralRange, range)
  def isUnion: Boolean                                       = nodesInRange.size > 1
  def isMandatory: Boolean                                   = minCount().option().getOrElse(0) == 1
  def withMinCount(minCount: Int): this.type                 = set(meta.MinCount, minCount)
  def withPattern(pattern: String): this.type                = set(meta.Pattern, pattern)
  def withMinimum(min: Double): this.type                    = set(meta.Minimum, min)
  def withMaximum(max: Double): this.type                    = set(meta.Maximum, max)
  def withAllowMultiple(allow: Boolean): this.type           = set(meta.AllowMultiple, allow)
  def withEnum(values: Seq[Any]): this.type                  = setArray(meta.Enum, values.map(AmfScalar(_)))
  def withSorted(sorted: Boolean): this.type                 = set(meta.Sorted, sorted)
  def withUnique(unique: Boolean): this.type                 = set(meta.Unique, unique)
  def withExternallyLinkable(linkable: Boolean): this.type   = set(meta.ExternallyLinkable, linkable)

  def meta: M
}
