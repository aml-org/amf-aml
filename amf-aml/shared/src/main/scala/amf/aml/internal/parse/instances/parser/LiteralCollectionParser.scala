package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.parse.instances.DialectInstanceContext
import org.yaml.model.{YMapEntry, YSequence, YType}

object LiteralCollectionParser {

  def parse(propertyEntry: YMapEntry, property: PropertyLikeMapping[_], node: DialectDomainElement)(implicit
      ctx: DialectInstanceContext
  ): Unit = {
    val finalValues = propertyEntry.value.tagType match {
      case YType.Seq =>
        val values = propertyEntry.value
          .as[YSequence]
          .nodes
          .flatMap { elemValue =>
            LiteralValueParser.parseLiteralValue(elemValue, property, node)(ctx.eh)
          }

        values.headOption match {
          case Some(("link", _: String)) => values.collect { case (_, link) => link }.asInstanceOf[Seq[String]]
          case _                         => values
        }

      case _ =>
        LiteralValueParser.parseLiteralValue(propertyEntry.value, property, node)(ctx.eh) match {
          case Some(("link", v)) => Seq(v)
          case Some(v)           => Seq(v)
          case _                 => Nil
        }

    }
    node.setProperty(property, finalValues, propertyEntry)

  }
}
