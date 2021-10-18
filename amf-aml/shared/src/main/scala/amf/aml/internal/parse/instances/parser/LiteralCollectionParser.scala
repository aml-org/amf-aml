package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, PropertyMapping}
import amf.aml.internal.parse.instances.DialectInstanceContext
import org.yaml.model.{YMapEntry, YSequence, YType}

object LiteralCollectionParser {

  def parse(propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement)(
      implicit ctx: DialectInstanceContext): Unit = {
    val finalValues = propertyEntry.value.tagType match {
      case YType.Seq =>
        val values = propertyEntry.value
          .as[YSequence]
          .nodes
          .flatMap { elemValue =>
            LiteralValueParser.parseLiteralValue(elemValue, property, node)
          }

        values.headOption match {
          case Some(("link", _: String)) => values.collect { case (_, link) => link }.asInstanceOf[Seq[String]]
          case _                         => values
        }

      case _ =>
        LiteralValueParser.parseLiteralValue(propertyEntry.value, property, node) match {
          case Some(("link", v)) => Seq(v)
          case Some(v)           => Seq(v)
          case _                 => Nil
        }

    }
    node.setProperty(property, finalValues, propertyEntry)

  }
}
