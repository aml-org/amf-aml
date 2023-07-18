package amf.aml.client.scala.model.domain

import amf.aml.internal.metamodel.domain.PropertyLikeMappingModel
import amf.core.client.scala.model._
import amf.core.client.scala.model.domain.{AmfScalar, DomainElement}
import amf.core.internal.metamodel.Field
import amf.core.internal.parser.domain.Annotations

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
  def mandatory(): BoolField          = fields.field(meta.Mandatory)
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
  def withMandatory(mandatory: Boolean): this.type           = set(meta.Mandatory, mandatory)

  def mapKeyProperty(): StrField = fields.field(meta.MapKeyProperty)

  def mapValueProperty(): StrField = fields.field(meta.MapValueProperty)

  def mapTermKeyProperty(): StrField = fields.field(meta.MapTermKeyProperty)

  def mapTermValueProperty(): StrField = fields.field(meta.MapTermValueProperty)

  def withMapKeyProperty(key: String, annotations: Annotations = Annotations()): this.type =
    set(meta.MapKeyProperty, AmfScalar(key, annotations))

  def withMapValueProperty(value: String, annotations: Annotations = Annotations()): this.type =
    set(meta.MapValueProperty, AmfScalar(value, annotations))

  def withMapTermKeyProperty(key: String, annotations: Annotations = Annotations()): this.type =
    set(meta.MapTermKeyProperty, AmfScalar(key, annotations))

  def withMapTermValueProperty(value: String, annotations: Annotations = Annotations()): this.type =
    set(meta.MapTermValueProperty, AmfScalar(value, annotations))

  def classification(): PropertyClassification = PropertyLikeMappingClassifier.classification(this)

  def toField(): Field = PropertyLikeMappingToFieldConverter.convert(this)

  private[amf] def isMultiple: Boolean = allowMultiple().option().getOrElse(false)

  def meta: M
}
