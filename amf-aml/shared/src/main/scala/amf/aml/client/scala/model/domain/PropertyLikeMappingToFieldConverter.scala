package amf.aml.client.scala.model.domain

import amf.aml.internal.metamodel.domain.DialectDomainElementModel
import amf.core.client.scala.model.DataType
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.{Field, Type}

object PropertyLikeMappingToFieldConverter {

  def convert(propertyLike: PropertyLikeMapping[_]): Field = {
    val iri = propertyLike
      .nodePropertyMapping()
      .option()
      .getOrElse((Namespace.Data + propertyLike.name().value()).iri())

    val propertyIdValue = ValueType(iri)
    val isObjectRange   = propertyLike.objectRange().nonEmpty || Option(propertyLike.typeDiscriminator()).isDefined

    if (isObjectRange) {
      if (propertyLike.allowMultiple().value() && propertyLike.sorted().value()) {
        Field(Type.SortedArray(DialectDomainElementModel()), propertyIdValue)
      } else if (propertyLike.allowMultiple().value() || hasMapTermKey(propertyLike)) {
        Field(Type.Array(DialectDomainElementModel()), propertyIdValue)
      } else {
        Field(DialectDomainElementModel(), propertyIdValue)
      }
    } else {
      val fieldType = propertyLike.literalRange().option() match {
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

      if (propertyLike.allowMultiple().value()) {
        Field(Type.Array(fieldType), propertyIdValue)
      } else {
        Field(fieldType, propertyIdValue)
      }
    }
  }

  private def hasMapTermKey(propertyLikeMapping: PropertyLikeMapping[_]): Boolean = propertyLikeMapping match {
    case mapping: PropertyMapping => mapping.mapTermKeyProperty().nonNull
    case _                        => false
  }
}
