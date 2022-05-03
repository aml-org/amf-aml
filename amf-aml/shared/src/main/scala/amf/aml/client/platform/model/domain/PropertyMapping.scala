package amf.aml.client.platform.model.domain

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.{AnyField, BoolField, DoubleField, IntField, StrField}
import amf.core.client.platform.model.domain.DomainElement
import amf.aml.client.scala.model.domain.{
  ExtensionPointProperty,
  LiteralProperty,
  LiteralPropertyCollection,
  ObjectMapInheritanceProperty,
  ObjectMapProperty,
  ObjectPairProperty,
  ObjectProperty,
  ObjectPropertyCollection,
  PropertyMapping => InternalPropertyMapping
}

import scala.collection.mutable
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class PropertyMapping(override private[amf] val _internal: InternalPropertyMapping) extends DomainElement {

  @JSExportTopLevel("PropertyMapping")
  def this() = this(InternalPropertyMapping())

  def name(): StrField                    = _internal.name()
  def nodePropertyMapping(): StrField     = _internal.nodePropertyMapping()
  def literalRange(): StrField            = _internal.literalRange()
  def objectRange(): ClientList[StrField] = _internal.objectRange().asClient
  def mapKeyProperty(): StrField          = _internal.mapKeyProperty()
  def mapValueProperty(): StrField        = _internal.mapKeyProperty()
  def minCount(): IntField                = _internal.minCount()
  def pattern(): StrField                 = _internal.pattern()
  def minimum(): DoubleField              = _internal.minimum()
  def maximum(): DoubleField              = _internal.maximum()
  def allowMultiple(): BoolField          = _internal.allowMultiple()
  def enum(): ClientList[AnyField]        = _internal.enum().asClient
  def sorted(): BoolField                 = _internal.sorted()
  def typeDiscriminatorName(): StrField   = _internal.typeDiscriminatorName()
  def externallyLinkable(): BoolField     = _internal.externallyLinkable()
  def mandatory(): BoolField              = _internal.mandatory()
  def typeDiscriminator(): ClientMap[String] = Option(_internal.typeDiscriminator()) match {
    case Some(m) =>
      m.foldLeft(mutable.Map[String, String]()) { case (acc, (k, v)) =>
        acc.put(k, v)
        acc
      }.asClient
    case None => mutable.Map[String, String]().asClient
  }

  def withName(name: String): PropertyMapping = {
    _internal.withName(name)
    this
  }
  def withNodePropertyMapping(propertyId: String): PropertyMapping = {
    _internal.withNodePropertyMapping(propertyId)
    this
  }
  def withLiteralRange(range: String): PropertyMapping = {
    _internal.withLiteralRange(range)
    this
  }
  def withObjectRange(range: ClientList[String]): PropertyMapping = {
    _internal.withObjectRange(range.asInternal)
    this
  }
  def withMapKeyProperty(key: String): PropertyMapping = {
    _internal.withMapKeyProperty(key)
    this
  }
  def withMapValueProperty(value: String): PropertyMapping = {
    _internal.withMapValueProperty(value)
    this
  }
  def withMinCount(minCount: Int): PropertyMapping = {
    _internal.withMinCount(minCount)
    this
  }
  def withPattern(pattern: String): PropertyMapping = {
    _internal.withPattern(pattern)
    this
  }
  def withMinimum(min: Double): PropertyMapping = {
    _internal.withMinimum(min)
    this
  }
  def withMaximum(max: Double): PropertyMapping = {
    _internal.withMaximum(max)
    this
  }
  def withAllowMultiple(allow: Boolean): PropertyMapping = {
    _internal.withAllowMultiple(allow)
    this
  }
  def withEnum(values: ClientList[Any]): PropertyMapping = {
    _internal.withEnum(values.asInternal)
    this
  }
  def withSorted(sorted: Boolean): PropertyMapping = {
    _internal.withSorted(sorted)
    this
  }
  def withTypeDiscriminator(typesMapping: ClientMap[String]): PropertyMapping = {
    _internal.withTypeDiscriminator(typesMapping.asInternal)
    this
  }
  def withTypeDiscriminatorName(name: String): PropertyMapping = {
    _internal.withTypeDiscriminatorName(name)
    this
  }
  def withExternallyLinkable(linkable: Boolean): PropertyMapping = {
    _internal.withExternallyLinkable(linkable)
    this
  }
  def withMandatory(mandatory: Boolean): PropertyMapping = {
    _internal.withMandatory(mandatory)
    this
  }

  def classification(): String = {
    _internal.classification() match {
      case ExtensionPointProperty       => "extension_property"
      case LiteralProperty              => "literal_property"
      case ObjectProperty               => "object_property"
      case ObjectPropertyCollection     => "object_property_collection"
      case ObjectMapProperty            => "object_map_property"
      case ObjectMapInheritanceProperty => "object_map_inheritance"
      case ObjectPairProperty           => "object_pair_property"
      case LiteralPropertyCollection    => "literal_property_collection"
      case other                        => throw new Exception(s"Unknown property classification ${other}")
    }
  }

}
