package amf.plugins.document.vocabularies.emitters.dialects

import amf.core.annotations.LexicalInformation
import amf.core.emitter.BaseEmitters.{ArrayEmitter, MapEntryEmitter, ScalarEmitter}
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.model.domain.AmfScalar
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.core.vocabulary.Namespace
import amf.plugins.document.vocabularies.emitters.instances.NodeMappableFinder
import amf.plugins.document.vocabularies.metamodel.domain.PropertyMappingModel
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.PropertyMapping
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YType

case class PropertyMappingEmitter(
    dialect: Dialect,
    propertyMapping: PropertyMapping,
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)])(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with DiscriminatorEmitter
    with AliasesConsumer
    with PosExtractor {
  override def emit(b: EntryBuilder): Unit = {
    b.entry(
        propertyMapping.name().value(),
        _.obj { b =>
          var emitters: Seq[EntryEmitter] = Seq()

          aliasFor(propertyMapping.nodePropertyMapping().value()) match {
            case Some(propertyTermAlias) =>
              val pos = fieldPos(propertyMapping, PropertyMappingModel.NodePropertyMapping)
              emitters ++= Seq(MapEntryEmitter("propertyTerm", propertyTermAlias, YType.Str, pos))
            case None =>
          }

          propertyMapping.literalRange().option().foreach {
            case literal if literal == (Namespace.Shapes + "guid").iri() =>
              val pos = fieldPos(propertyMapping, PropertyMappingModel.LiteralRange)
              emitters ++= Seq(MapEntryEmitter("range", "guid", YType.Str, pos))

            case literal if literal.endsWith("anyURI") =>
              val pos = fieldPos(propertyMapping, PropertyMappingModel.LiteralRange)
              emitters ++= Seq(MapEntryEmitter("range", "uri", YType.Str, pos))

            case literal if literal.endsWith("link") =>
              val pos = fieldPos(propertyMapping, PropertyMappingModel.LiteralRange)
              emitters ++= Seq(MapEntryEmitter("range", "link", YType.Str, pos))

            case literal if literal.endsWith("anyType") =>
              val pos = fieldPos(propertyMapping, PropertyMappingModel.LiteralRange)
              emitters ++= Seq(MapEntryEmitter("range", "any", YType.Str, pos))

            case literal if literal.endsWith("number") =>
              val pos = fieldPos(propertyMapping, PropertyMappingModel.LiteralRange)
              emitters ++= Seq(MapEntryEmitter("range", "number", YType.Str, pos))

            case literal =>
              val pos = fieldPos(propertyMapping, PropertyMappingModel.LiteralRange)
              emitters ++= Seq(MapEntryEmitter("range", literal.split(Namespace.Xsd.base).last, YType.Str, pos))
          }

          propertyMapping.mergePolicy.option().foreach { policy =>
            val pos = fieldPos(propertyMapping, PropertyMappingModel.MergePolicy)
            emitters ++= Seq(
                new MapEntryEmitter("patch", policy, YType.Str, pos)
            )
          }

          val nodes = propertyMapping.objectRange()
          if (nodes.nonEmpty) {
            val pos = fieldPos(propertyMapping, PropertyMappingModel.ObjectRange)
            val targets = nodes
              .map { nodeId =>
                if (nodeId.value() == (Namespace.Meta + "anyNode").iri()) {
                  Some("anyNode")
                } else {
                  aliasFor(nodeId.value()) match {
                    case Some(nodeMappingAlias) => Some(nodeMappingAlias)
                    case _                      => None
                  }
                }
              }
              .collect { case Some(alias) => alias }

            if (targets.size == 1)
              emitters ++= Seq(MapEntryEmitter("range", targets.head, YType.Str, pos))
            else if (targets.size > 1)
              emitters ++= Seq(new EntryEmitter {
                override def emit(b: EntryBuilder): Unit =
                  b.entry("range", _.list { b =>
                    targets.foreach(target => ScalarEmitter(AmfScalar(target)).emit(b))
                  })
                override def position(): Position = pos
              })
          }

          propertyMapping
            .mapKeyProperty()
            .option()
            .fold({
              propertyMapping.mapTermKeyProperty().option().foreach { term =>
                val pos = fieldPos(propertyMapping, PropertyMappingModel.MapTermKeyProperty)
                aliasFor(term) match {
                  case Some(propertyId) => emitters ++= Seq(MapEntryEmitter("mapTermKey", propertyId, YType.Str, pos))
                  case _                =>
                }
              }
            })({ value =>
              val pos = fieldPos(propertyMapping, PropertyMappingModel.MapKeyProperty)
              emitters ++= Seq(MapEntryEmitter("mapKey", value, YType.Str, pos))
            })

          propertyMapping
            .mapValueProperty()
            .option()
            .fold({
              propertyMapping.mapTermValueProperty().option().foreach { term =>
                val pos = fieldPos(propertyMapping, PropertyMappingModel.MapTermValueProperty)
                aliasFor(term) match {
                  case Some(propertyId) =>
                    emitters ++= Seq(MapEntryEmitter("mapTermValue", propertyId, YType.Str, pos))
                  case _ =>
                }
              }
            })({ value =>
              val pos = fieldPos(propertyMapping, PropertyMappingModel.MapValueProperty)
              emitters ++= Seq(MapEntryEmitter("mapValue", value, YType.Str, pos))
            })

          propertyMapping.unique().option().foreach { value =>
            val pos = fieldPos(propertyMapping, PropertyMappingModel.Unique)
            emitters ++= Seq(MapEntryEmitter("unique", value.toString, YType.Bool, pos))
          }

          propertyMapping.fields.entry(PropertyMappingModel.MinCount) foreach { entry =>
            val value = entry.value.value.asInstanceOf[AmfScalar].value
            val pos   = fieldPos(propertyMapping, entry.field)
            value match {
              case 0 => emitters ++= Seq(MapEntryEmitter("mandatory", "false", YType.Bool, pos))
              case 1 => emitters ++= Seq(MapEntryEmitter("mandatory", "true", YType.Bool, pos))
            }
          }

          propertyMapping.fields.entry(PropertyMappingModel.Pattern) foreach { entry =>
            val value = entry.value.value.asInstanceOf[AmfScalar].value.toString
            val pos   = fieldPos(propertyMapping, entry.field)
            emitters ++= Seq(MapEntryEmitter("pattern", value, YType.Str, pos))
          }

          propertyMapping.fields.entry(PropertyMappingModel.Minimum) foreach { entry =>
            val value = entry.value.value.asInstanceOf[AmfScalar].value
            val pos   = fieldPos(propertyMapping, entry.field)
            emitters ++= Seq(MapEntryEmitter("minimum", value.toString, YType.Int, pos))
          }

          propertyMapping.fields.entry(PropertyMappingModel.Maximum) foreach { entry =>
            val value = entry.value.value.asInstanceOf[AmfScalar].value
            val pos   = fieldPos(propertyMapping, entry.field)
            emitters ++= Seq(MapEntryEmitter("maximum", value.toString, YType.Int, pos))
          }

          propertyMapping.fields.entry(PropertyMappingModel.AllowMultiple) foreach { entry =>
            val value = entry.value.value.asInstanceOf[AmfScalar].value
            val pos   = fieldPos(propertyMapping, entry.field)
            emitters ++= Seq(MapEntryEmitter("allowMultiple", value.toString, YType.Bool, pos))
          }

          propertyMapping.fields.entry(PropertyMappingModel.Sorted) foreach { entry =>
            val value = entry.value.value.asInstanceOf[AmfScalar].value
            val pos   = fieldPos(propertyMapping, entry.field)
            emitters ++= Seq(MapEntryEmitter("sorted", value.toString, YType.Bool, pos))
          }

          propertyMapping.fields.entry(PropertyMappingModel.Enum) foreach { entry =>
            emitters ++= Seq(ArrayEmitter("enum", entry, ordering))
          }

          propertyMapping.fields.entry(PropertyMappingModel.ExternallyLinkable) foreach { entry =>
            val value = entry.value.value.asInstanceOf[AmfScalar].value
            val pos   = fieldPos(propertyMapping, entry.field)
            value match {
              case true  => emitters ++= Seq(MapEntryEmitter("isLink", "true", YType.Bool, pos))
              case false => emitters ++= Seq(MapEntryEmitter("isLink", "false", YType.Bool, pos))
            }
          }

          emitters ++= emitDiscriminator(propertyMapping)

          ordering.sorted(emitters).foreach(_.emit(b))
        }
    )
  }

  override def position(): Position =
    propertyMapping.annotations.find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)
}
