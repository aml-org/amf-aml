package amf.aml.client.scala.model.domain

import amf.core.client.scala.model._
import amf.core.client.scala.model.domain.AmfScalar
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.{Field, Type}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.domain.PropertyMappingModel._
import amf.aml.internal.metamodel.domain.{DialectDomainElementModel, PropertyMappingModel}
import org.yaml.model.YPart

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
    extends PropertyLikeMapping[PropertyMappingModel.type]
    with MergeableMapping
    with NodeWithDiscriminator[PropertyMappingModel.type] {

  def mapKeyProperty(): StrField   = fields.field(MapKeyProperty)
  def mapValueProperty(): StrField = fields.field(MapValueProperty)

  def mapTermKeyProperty(): StrField   = fields.field(MapTermKeyProperty)
  def mapTermValueProperty(): StrField = fields.field(MapTermValueProperty)

  def withMapKeyProperty(key: String, annotations: Annotations = Annotations()): PropertyMapping =
    set(MapKeyProperty, AmfScalar(key, annotations))
  def withMapValueProperty(value: String, annotations: Annotations = Annotations()): PropertyMapping =
    set(MapValueProperty, AmfScalar(value, annotations))
  def withMapTermKeyProperty(key: String, annotations: Annotations = Annotations()): PropertyMapping =
    set(MapTermKeyProperty, AmfScalar(key, annotations))
  def withMapTermValueProperty(value: String, annotations: Annotations = Annotations()): PropertyMapping =
    set(MapTermValueProperty, AmfScalar(value, annotations))

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

  def toField: Field = {
    val iri = nodePropertyMapping()
      .option()
      .getOrElse((Namespace.Data + name().value()).iri())

    val propertyIdValue = ValueType(iri)
    val isObjectRange   = objectRange().nonEmpty || Option(typeDiscriminator()).isDefined

    if (isObjectRange) {
      if (allowMultiple().value() && sorted().value()) {
        Field(Type.SortedArray(DialectDomainElementModel()), propertyIdValue)
      } else if (allowMultiple().value() || mapTermKeyProperty().nonNull) {
        Field(Type.Array(DialectDomainElementModel()), propertyIdValue)
      } else {
        Field(DialectDomainElementModel(), propertyIdValue)
      }
    } else {
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
      } else {
        Field(fieldType, propertyIdValue)
      }
    }
  }

  override def meta: PropertyMappingModel.type = PropertyMappingModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = ""
}

object PropertyMapping {
  def apply(): PropertyMapping = apply(Annotations())

  def apply(ast: YPart): PropertyMapping = apply(Annotations(ast))

  def apply(annotations: Annotations): PropertyMapping =
    PropertyMapping(Fields(), annotations)
}
