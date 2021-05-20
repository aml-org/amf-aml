package amf.plugins.document.vocabularies.parser.dialects.property.like

import amf.core.model.DataType
import amf.core.model.domain.AmfScalar
import amf.core.parser.{Annotations, ValueNode, YMapOps}
import amf.core.vocabulary.Namespace
import amf.plugins.document.vocabularies.model.domain.PropertyLikeMapping
import org.yaml.model.{YMap, YMapEntry, YType}
import amf.plugins.document.vocabularies.metamodel.domain.PropertyLikeMappingModel
import amf.plugins.document.vocabularies.metamodel.domain.PropertyMappingModel.LiteralRange

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
                  setLiteralRange((Namespace.Shapes + "guid").iri(), entry)
                case "string" | "integer" | "boolean" | "float" | "decimal" | "double" | "duration" | "dateTime" |
                    "time" | "date" | "anyType" =>
                  setLiteralRange((Namespace.Xsd + range).iri(), entry)
                case "anyUri"  => setLiteralRange(DataType.AnyUri, entry)
                case "link"    => setLiteralRange((Namespace.Shapes + "link").iri(), entry)
                case "number"  => setLiteralRange(DataType.Number, entry)
                case "uri"     => setLiteralRange(DataType.AnyUri, entry)
                case "any"     => setLiteralRange(DataType.Any, entry)
                case "anyNode" => propertyLikeMapping.withObjectRange(Seq((Namespace.Meta + "anyNode").iri()))
                case nodeMappingId =>
                  propertyLikeMapping
                    .withObjectRange(Seq(nodeMappingId)) // temporary until we can resolve all nodeMappings after finishing parsing declarations
              }
          }
        }
    )
  }

  private def setLiteralRange(iri: String, entry: YMapEntry) = {
    propertyLikeMapping.set(LiteralRange, AmfScalar(iri, Annotations(entry.value)), Annotations(entry))
  }
}
