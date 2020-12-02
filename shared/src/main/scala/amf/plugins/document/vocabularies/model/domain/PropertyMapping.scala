package amf.plugins.document.vocabularies.model.domain

import amf.core.metamodel.{Field, Obj, Type}
import amf.core.model._
import amf.core.model.domain.{AmfScalar, DomainElement}
import amf.core.parser.{Annotations, Fields}
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.metamodel.domain.PropertyMappingModel._
import amf.plugins.document.vocabularies.metamodel.domain.{DialectDomainElementModel, PropertyMappingModel}
import org.yaml.model.YMap

class PropertyClassification
object ExtensionPointProperty       extends PropertyClassification
object LiteralProperty              extends PropertyClassification
object ObjectProperty               extends PropertyClassification
object ObjectPropertyCollection     extends PropertyClassification
object ObjectMapProperty            extends PropertyClassification
object ObjectMapInheritanceProperty extends PropertyClassification
object ObjectPairProperty           extends PropertyClassification
object LiteralPropertyCollection    extends PropertyClassification
object ExternalLinkProperty         extends PropertyClassification

case class PropertyMapping(fields: Fields, annotations: Annotations)
    extends DomainElement
    with MergeableMapping
    with NodeWithDiscriminator[PropertyMapping] {

  def name(): StrField                = fields.field(Name)
  def nodePropertyMapping(): StrField = fields.field(NodePropertyMapping)

  def literalRange(): StrField     = fields.field(LiteralRange)
  def mapKeyProperty(): StrField   = fields.field(MapKeyProperty)
  def mapValueProperty(): StrField = fields.field(MapValueProperty)

  def mapTermKeyProperty(): StrField   = fields.field(MapTermKeyProperty)
  def mapTermValueProperty(): StrField = fields.field(MapTermValueProperty)

  def minCount(): IntField            = fields.field(MinCount)
  def pattern(): StrField             = fields.field(Pattern)
  def minimum(): DoubleField          = fields.field(Minimum)
  def maximum(): DoubleField          = fields.field(Maximum)
  def allowMultiple(): BoolField      = fields.field(AllowMultiple)
  def sorted(): BoolField             = fields.field(Sorted)
  def enum(): Seq[AnyField]           = fields.field(PropertyMappingModel.Enum)
  def unique(): BoolField             = fields.field(Unique)
  def externallyLinkable(): BoolField = fields.field(ExternallyLinkable)

  def withName(name: String): PropertyMapping = set(Name, name)
  def withNodePropertyMapping(propertyId: String): PropertyMapping =
    set(NodePropertyMapping, propertyId)
  def withLiteralRange(range: String): PropertyMapping =
    set(LiteralRange, range)
  def withMapKeyProperty(key: String): PropertyMapping =
    set(MapKeyProperty, key)
  def withMapValueProperty(value: String): PropertyMapping =
    set(MapValueProperty, value)
  def withMapTermKeyProperty(key: String): PropertyMapping =
    set(MapTermKeyProperty, key)
  def withMapTermValueProperty(value: String): PropertyMapping =
    set(MapTermValueProperty, value)
  def withMinCount(minCount: Int): PropertyMapping  = set(MinCount, minCount)
  def withPattern(pattern: String): PropertyMapping = set(Pattern, pattern)
  def withMinimum(min: Double): PropertyMapping     = set(Minimum, min)
  def withMaximum(max: Double): PropertyMapping     = set(Maximum, max)
  def withAllowMultiple(allow: Boolean): PropertyMapping =
    set(AllowMultiple, allow)
  def withEnum(values: Seq[Any]): PropertyMapping =
    setArray(PropertyMappingModel.Enum, values.map(AmfScalar(_)))
  def withSorted(sorted: Boolean): PropertyMapping = set(Sorted, sorted)
  def withUnique(unique: Boolean): PropertyMapping = set(Unique, unique)
  def withExternallyLinkable(linkable: Boolean): PropertyMapping = set(ExternallyLinkable, linkable)

  def classification(): PropertyClassification = {
    val isAnyNode = objectRange().exists { obj =>
      obj.value() == (Namespace.Meta + "anyNode").iri()
    }
    val isLiteral      = literalRange().nonNull
    val isObject       = objectRange().nonEmpty
    val multiple       = allowMultiple().option().getOrElse(false)
    val isMap          = mapTermKeyProperty().nonNull
    val isMapValue     = mapTermValueProperty().nonNull
    val isExternalLink = externallyLinkable().option().getOrElse(false)

    if (isExternalLink)
      ExternalLinkProperty
    else if (isAnyNode)
      ExtensionPointProperty
    else if (isLiteral && !multiple)
      LiteralProperty
    else if (isLiteral)
      LiteralPropertyCollection
    else if (isObject && isMap && isMapValue)
      ObjectPairProperty
    else if (isObject && isMap)
      ObjectMapProperty
    else if (isObject && !multiple)
      ObjectProperty
    else
      ObjectPropertyCollection
  }

  def nodesInRange: Seq[String] = {
    val range = objectRange()
    if (range.isEmpty) {
      Option(typeDiscriminator()).getOrElse(Map()).values.toSeq
    }
    else {
      range.map(_.value())
    }
  }

  def isUnion: Boolean = nodesInRange.size > 1

  def toField: Field = {
    val iri = nodePropertyMapping()
      .option()
      .getOrElse((Namespace.Data + name().value()).iri())

    val propertyIdValue = ValueType(iri)
    val isObjectRange   = objectRange().nonEmpty || Option(typeDiscriminator()).isDefined

    if (isObjectRange) {
      if (allowMultiple().value() && sorted().value()) {
        Field(Type.SortedArray(DialectDomainElementModel()), propertyIdValue)
      }
      else if (allowMultiple().value() || mapTermKeyProperty().nonNull) {
        Field(Type.Array(DialectDomainElementModel()), propertyIdValue)
      }
      else {
        Field(DialectDomainElementModel(), propertyIdValue)
      }
    }
    else {
      val fieldType = literalRange().option() match {
        case Some(literal) if literal == (Namespace.Shapes + "link").iri() => Type.Iri
        case Some(literal) if literal == DataType.AnyUri =>
          Type.LiteralUri
        case Some(literal) if literal.endsWith("anyType") => Type.Any
        case Some(literal) if literal.endsWith("number")  => Type.Float
        case Some(literal) if literal == DataType.Integer => Type.Int
        case Some(literal) if literal == DataType.Float   => Type.Float
        case Some(literal) if literal == DataType.Double =>
          Type.Double
        case Some(literal) if literal == DataType.Boolean =>
          Type.Bool
        case Some(literal) if literal == DataType.Decimal => Type.Int
        case Some(literal) if literal == DataType.Time    => Type.Time
        case Some(literal) if literal == DataType.Date    => Type.Date
        case Some(literal) if literal == DataType.DateTime =>
          Type.Date
        case _ => Type.Str
      }

      if (allowMultiple().value()) {
        Field(Type.Array(fieldType), propertyIdValue)
      }
      else {
        Field(fieldType, propertyIdValue)
      }
    }
  }

  override def meta: Obj = PropertyMappingModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = ""
}

object PropertyMapping {
  def apply(): PropertyMapping = apply(Annotations())

  def apply(ast: YMap): PropertyMapping = apply(Annotations(ast))

  def apply(annotations: Annotations): PropertyMapping =
    PropertyMapping(Fields(), annotations)
}
