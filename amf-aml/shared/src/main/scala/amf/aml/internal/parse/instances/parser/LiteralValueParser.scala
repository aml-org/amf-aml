package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, PropertyLikeMapping}
import amf.aml.internal.validate.DialectValidations.{DialectError, InconsistentPropertyRangeValueSpecification}
import amf.core.client.common.position.Range
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.DataType
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.plugins.syntax.{SYamlBasedErrorHandler, SyamlAMFErrorHandler}
import amf.core.internal.utils.AmfStrings
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.{YMapEntry, YNode, YScalar, YType}

object LiteralValueSetter {
  def setLiteralValue(
      parsed: Option[_],
      entry: YMapEntry,
      property: PropertyLikeMapping[_],
      node: DialectDomainElement
  ): Unit = {
    parsed match {
      case Some(b: Boolean)          => node.setProperty(property, b, entry)
      case Some(i: Int)              => node.setProperty(property, i, entry)
      case Some(f: Float)            => node.setProperty(property, f, entry)
      case Some(d: Double)           => node.setProperty(property, d, entry)
      case Some(l: Long)             => node.setProperty(property, l, entry)
      case Some(s: String)           => node.setProperty(property, s, entry)
      case Some(("link", l: String)) => node.setProperty(property, l, entry)
      case Some(d: SimpleDateTime)   => node.setProperty(property, d, entry)
      case _                         => node.setProperty(property, entry)
    }
  }
}

object LiteralValueParser {

  def parseLiteralValue(value: YNode, property: PropertyLikeMapping[_], element: DialectDomainElement)(implicit
      eh: AMFErrorHandler
  ): Option[_] = {
    parseLiteralValue(value, property, element.id, Annotations(value))(new SyamlAMFErrorHandler(eh))
  }

  def parseLiteralValue(value: YNode, property: PropertyLikeMapping[_], nodeId: String, annotations: Annotations)(
      implicit eh: SyamlAMFErrorHandler
  ): Option[_] = {

    value.tagType match {
      case YType.Bool
          if (property.literalRange().value() == DataType.Boolean) || property
            .literalRange()
            .value() == DataType.Any =>
        Some(value.as[Boolean])
      case YType.Bool =>
        inconsistentPropertyRangeValueViolation(
            nodeId,
            property,
            property.literalRange().value(),
            DataType.Boolean,
            annotations
        )(eh)
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
      case YType.Int if property.literalRange().value() == DataType.Long =>
        Some(value.as[Long])
      case YType.Int =>
        inconsistentPropertyRangeValueViolation(
            nodeId,
            property,
            property.literalRange().value(),
            DataType.Integer,
            annotations
        )(eh)
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
        inconsistentPropertyRangeValueViolation(
            nodeId,
            property,
            property.literalRange().value(),
            DataType.String,
            annotations
        )(eh)
        None
      case YType.Float
          if property.literalRange().value() == DataType.Float ||
            property.literalRange().value() == DataType.Number ||
            property.literalRange().value() == DataType.Double ||
            property.literalRange().value() == DataType.Any =>
        Some(value.as[Double])
      case YType.Float =>
        inconsistentPropertyRangeValueViolation(
            nodeId,
            property,
            property.literalRange().value(),
            DataType.Float,
            annotations
        )(eh)
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
        inconsistentPropertyRangeValueViolation(
            nodeId,
            property,
            property.literalRange().value(),
            DataType.DateTime,
            annotations
        )(eh)
        Some(value.as[String])

      case YType.Null =>
        None
      case _ =>
        eh.violation(DialectError, nodeId, s"Unsupported scalar type ${value.tagType}", annotations)
        Some(value.as[String])
    }
  }

  private def inconsistentPropertyRangeValueViolation(
      node: String,
      property: PropertyLikeMapping[_],
      expected: String,
      found: String,
      annotations: Annotations
  )(eh: SyamlAMFErrorHandler): Unit = {
    eh.violation(
        InconsistentPropertyRangeValueSpecification,
        node,
        Some(property.nodePropertyMapping().value()),
        s"Cannot find expected range for property ${property.nodePropertyMapping().value()} (${property.name().value()}). Found '$found', expected '$expected'",
        Some(LexicalInformation(annotations.lexical())),
        annotations.location()
    )

  }
}
