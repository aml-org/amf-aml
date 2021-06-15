package amf.plugins.document.vocabularies.parser.dialects.property.like

import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, ScalarNode, ValueNode}
import amf.plugins.document.vocabularies.metamodel.domain.PropertyLikeMappingModel
import amf.plugins.document.vocabularies.metamodel.domain.PropertyMappingModel.{LiteralRange, ObjectRange}
import amf.plugins.document.vocabularies.model.domain.PropertyLikeMapping
import org.yaml.model.{YMap, YMapEntry, YSequence, YType}

case class RangeParser(map: YMap, propertyLikeMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel]) {
  def parse(): Unit = {
    map.key(
        "range",
        entry => {
          entry.value.tagType match {
            case YType.Seq =>
              val scalars = entry.value.as[YSequence].nodes.map(ScalarNode(_).text())
              propertyLikeMapping.set(ObjectRange, AmfArray(scalars, Annotations(entry.value)), Annotations(entry))
            case _ =>
              val value = ValueNode(entry.value)
              val range = value.string().toString
              range match {
                case "guid" =>
                  setLiteralRange((Namespace.Shapes + "guid").iri(), entry)
                case "string" | "integer" | "boolean" | "float" | "decimal" | "double" | "duration" | "dateTime" |
                    "time" | "date" | "anyType" =>
                  setLiteralRange((Namespace.Xsd + range).iri(), entry)
                case "anyUri"      => setLiteralRange(DataType.AnyUri, entry)
                case "link"        => setLiteralRange((Namespace.Shapes + "link").iri(), entry)
                case "number"      => setLiteralRange(DataType.Number, entry)
                case "uri"         => setLiteralRange(DataType.AnyUri, entry)
                case "any"         => setLiteralRange(DataType.Any, entry)
                case "anyNode"     => propertyLikeMapping.withObjectRange(Seq((Namespace.Meta + "anyNode").iri()))
                case nodeMappingId =>
                  // temporary until we can resolve all nodeMappings after finishing parsing declarations
                  propertyLikeMapping.set(
                      ObjectRange,
                      AmfArray(Seq(AmfScalar(nodeMappingId, Annotations(entry.value))), Annotations.virtual()),
                      Annotations(entry))
              }
          }
        }
    )
  }

  private def setLiteralRange(iri: String, entry: YMapEntry) = {
    propertyLikeMapping.set(LiteralRange, AmfScalar(iri, Annotations(entry.value)), Annotations(entry))
  }
}
