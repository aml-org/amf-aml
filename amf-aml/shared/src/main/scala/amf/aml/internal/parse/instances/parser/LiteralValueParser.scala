package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, PropertyMapping}
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.DataType
import amf.core.client.scala.vocabulary.Namespace
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.{YMapEntry, YNode, YScalar, YType}

object LiteralValueSetter {
  def setLiteralValue(parsed: Option[_],
                      entry: YMapEntry,
                      property: PropertyMapping,
                      node: DialectDomainElement): Unit = {
    parsed match {
      case Some(b: Boolean)          => node.setProperty(property, b, entry)
      case Some(i: Int)              => node.setProperty(property, i, entry)
      case Some(f: Float)            => node.setProperty(property, f, entry)
      case Some(d: Double)           => node.setProperty(property, d, entry)
      case Some(s: String)           => node.setProperty(property, s, entry)
      case Some(("link", l: String)) => node.setProperty(property, l, entry)
      case Some(d: SimpleDateTime)   => node.setProperty(property, d, entry)
      case _                         => node.setProperty(property, entry)
    }
  }
}

object LiteralValueParser {

  def parseLiteralValue(value: YNode, property: PropertyMapping, node: DialectDomainElement)(
      implicit ctx: DialectInstanceContext): Option[_] = {

    value.tagType match {
      case YType.Bool
          if (property.literalRange().value() == DataType.Boolean) || property
            .literalRange()
            .value() == DataType.Any =>
        Some(value.as[Boolean])
      case YType.Bool =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    DataType.Boolean,
                                                    value)
        None
      case YType.Int
          if property.literalRange().value() == DataType.Integer || property
            .literalRange()
            .value() == DataType.Number || property.literalRange().value() == DataType.Any =>
        Some(value.as[Int])
      case YType.Int
          if property.literalRange().value() == DataType.Float ||
            property.literalRange().value() == DataType.Double =>
        Some(value.as[Double])
      case YType.Int =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    DataType.Integer,
                                                    value)
        None
      case YType.Str
          if property.literalRange().value() == DataType.String || property.literalRange().value() == DataType.Any =>
        Some(value.as[YScalar].text)
      case YType.Str if property.literalRange().value() == DataType.AnyUri =>
        Some(value.as[YScalar].text)
      case YType.Str if property.literalRange().value() == (Namespace.Shapes + "link").iri() =>
        Some(("link", value.as[YScalar].text))
      case YType.Str
          if property.literalRange().value() == DataType.Time ||
            property.literalRange().value() == DataType.Date ||
            property.literalRange().value() == DataType.DateTime =>
        Some(YNode(value.value, YType.Timestamp).as[SimpleDateTime])
      case YType.Str if property.literalRange().value() == (Namespace.Shapes + "guid").iri() =>
        Some(value.as[YScalar].text)
      case YType.Str =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    DataType.String,
                                                    value)
        None
      case YType.Float
          if property.literalRange().value() == DataType.Float ||
            property.literalRange().value() == DataType.Number ||
            property.literalRange().value() == DataType.Double ||
            property.literalRange().value() == DataType.Any =>
        Some(value.as[Double])
      case YType.Float =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    DataType.Float,
                                                    value)
        None

      case YType.Timestamp
          if property.literalRange().value() == DataType.Time ||
            property.literalRange().value() == DataType.Date ||
            property.literalRange().value() == DataType.DateTime ||
            property.literalRange().value() == DataType.Any =>
        Some(value.as[SimpleDateTime])

      case YType.Timestamp if property.literalRange().value() == DataType.String =>
        Some(value.as[YScalar].text)

      case YType.Timestamp =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    DataType.DateTime,
                                                    value)
        Some(value.as[String])

      case YType.Null =>
        None
      case _ =>
        ctx.eh.violation(DialectError, node.id, s"Unsupported scalar type ${value.tagType}", value.location)
        Some(value.as[String])
    }
  }
}
