package amf.plugins.document.vocabularies.parser.dialects.property.like

import amf.core.model.DataType
import amf.core.parser.{ValueNode, YMapOps}
import amf.core.vocabulary.Namespace
import amf.plugins.document.vocabularies.metamodel.domain.PropertyLikeMappingModel
import amf.plugins.document.vocabularies.model.domain.PropertyLikeMapping
import org.yaml.model.{YMap, YType}

case class RangeParser(map: YMap, propertyLikeMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel]) {
  def parse(): Unit = {
    map.key(
        "range",
        entry => {
          entry.value.tagType match {
            case YType.Seq =>
              propertyLikeMapping.withObjectRange(entry.value.as[Seq[String]])
            case _ =>
              val value = ValueNode(entry.value)
              val range = value.string().toString
              range match {
                case "guid" =>
                  propertyLikeMapping.withLiteralRange((Namespace.Shapes + "guid").iri())
                case "string" | "integer" | "boolean" | "float" | "decimal" | "double" | "duration" | "dateTime" |
                    "time" | "date" | "anyType" =>
                  propertyLikeMapping.withLiteralRange((Namespace.Xsd + range).iri())
                case "anyUri"  => propertyLikeMapping.withLiteralRange(DataType.AnyUri)
                case "link"    => propertyLikeMapping.withLiteralRange((Namespace.Shapes + "link").iri())
                case "number"  => propertyLikeMapping.withLiteralRange(DataType.Number)
                case "uri"     => propertyLikeMapping.withLiteralRange(DataType.AnyUri)
                case "any"     => propertyLikeMapping.withLiteralRange(DataType.Any)
                case "anyNode" => propertyLikeMapping.withObjectRange(Seq((Namespace.Meta + "anyNode").iri()))
                case nodeMappingId =>
                  propertyLikeMapping
                    .withObjectRange(Seq(nodeMappingId)) // temporary until we can resolve all nodeMappings after finishing parsing declarations
              }
          }
        }
    )
  }
}
