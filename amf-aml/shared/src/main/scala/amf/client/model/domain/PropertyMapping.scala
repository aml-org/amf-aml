package amf.client.model.domain

import amf.client.convert.VocabulariesClientConverter._
import amf.client.model._
import amf.plugins.document.vocabularies.model.domain.{
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

  @JSExportTopLevel("model.domain.PropertyMapping")
  def this() = this(InternalPropertyMapping())

  def withName(name: String): PropertyMapping = {
    _internal.withName(name)
    this
  }
  def name(): StrField = _internal.name()
  def withNodePropertyMapping(propertyId: String): PropertyMapping = {
    _internal.withNodePropertyMapping(propertyId)
    this
  }
  def nodePropertyMapping(): StrField = _internal.nodePropertyMapping()
  def withLiteralRange(range: String): PropertyMapping = {
    _internal.withLiteralRange(range)
    this
  }
  def literalRange(): StrField = _internal.literalRange()
  def withObjectRange(range: ClientList[String]): PropertyMapping = {
    _internal.withObjectRange(range.asInternal)
    this
  }
  def objectRange(): ClientList[StrField] = _internal.objectRange().asClient
  def mapKeyProperty(): StrField          = _internal.mapKeyProperty()
  def withMapKeyProperty(key: String): PropertyMapping = {
    _internal.withMapKeyProperty(key)
    this
  }
  def mapValueProperty(): StrField = _internal.mapKeyProperty()
  def withMapValueProperty(value: String): PropertyMapping = {
    _internal.withMapValueProperty(value)
    this
  }
  def minCount(): IntField = _internal.minCount()
  def withMinCount(minCount: Int): PropertyMapping = {
    _internal.withMinCount(minCount)
    this
  }
  def pattern(): StrField = _internal.pattern()
  def withPattern(pattern: String): PropertyMapping = {
    _internal.withPattern(pattern)
    this
  }
  def minimum(): DoubleField = _internal.minimum()
  def withMinimum(min: Double): PropertyMapping = {
    _internal.withMinimum(min)
    this
  }
  def maximum(): DoubleField = _internal.maximum()
  def withMaximum(max: Double): PropertyMapping = {
    _internal.withMaximum(max)
    this
  }
  def allowMultiple(): BoolField = _internal.allowMultiple()
  def withAllowMultiple(allow: Boolean): PropertyMapping = {
    _internal.withAllowMultiple(allow)
    this
  }
  def enum(): ClientList[AnyField] = _internal.enum().asClient
  def withEnum(values: ClientList[Any]): PropertyMapping = {
    _internal.withEnum(values.asInternal)
    this
  }
  def sorted(): BoolField = _internal.sorted()
  def withSorted(sorted: Boolean): PropertyMapping = {
    _internal.withSorted(sorted)
    this
  }
  def typeDiscriminator(): ClientMap[String] = Option(_internal.typeDiscriminator()) match {
    case Some(m) =>
      m.foldLeft(mutable.Map[String, String]()) {
          case (acc, (k, v)) =>
            acc.put(k, v)
            acc
        }
        .asClient
    case None => mutable.Map[String, String]().asClient
  }

  def withTypeDiscriminator(typesMapping: ClientMap[String]) = throw new Exception("Not implemented yet")
  def typeDiscriminatorName(): StrField                      = _internal.typeDiscriminatorName()
  def withTypeDiscriminatorName(name: String): PropertyMapping = {
    _internal.withTypeDiscriminatorName(name)
    this
  }

  def withExternallyLinkable(linkable: Boolean): InternalPropertyMapping = _internal.withExternallyLinkable(linkable);
  def externallyLinkable(): BoolField                                    = _internal.externallyLinkable()

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
