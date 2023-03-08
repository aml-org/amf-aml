package amf.aml.internal.render.emitters.dialects

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.metamodel.domain.{PropertyLikeMappingModel, PropertyMappingModel}
import amf.aml.internal.render.emitters.instances.NodeMappableFinder
import amf.core.client.scala.model.domain.AmfScalar
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.parser.domain.{Annotations, FieldEntry}
import amf.core.internal.render.BaseEmitters.{MapEntryEmitter, ScalarEmitter, pos, traverse}
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import org.mulesoft.common.client.lexical.Position
import org.mulesoft.common.client.lexical.Position.ZERO
import org.mulesoft.common.collections.FilterType
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YType

case class PropertyRangeEmitters[T <: PropertyLikeMappingModel](
    dialect: Dialect,
    propertyMapping: PropertyLikeMapping[T],
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)]
)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends AliasesConsumer
    with PosExtractor {

  def emitters: Seq[EntryEmitter] = {
    var emitters: Seq[EntryEmitter] = Seq()
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
            b.entry(
              "range",
              _.list { b =>
                targets.foreach(target => ScalarEmitter(AmfScalar(target)).emit(b))
              }
            )
          override def position(): Position = pos
        })
    }

    emitters
  }
}

case class PropertyLikeMappingEmitter[T <: PropertyLikeMappingModel](
    dialect: Dialect,
    propertyLikeMapping: PropertyLikeMapping[T],
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)]
)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends AliasesConsumer
    with PosExtractor
    with DiscriminatorEmitter {
  def emitters: Seq[EntryEmitter] = {

    var result = PropertyRangeEmitters(dialect, propertyLikeMapping, ordering, aliases).emitters

    aliasFor(propertyLikeMapping.nodePropertyMapping().value()) match {
      case Some(propertyTermAlias) =>
        val pos = fieldPos(propertyLikeMapping, PropertyMappingModel.NodePropertyMapping)
        result ++= Seq(MapEntryEmitter("propertyTerm", propertyTermAlias, YType.Str, pos))
      case None =>
    }

    propertyLikeMapping.unique().option().foreach { value =>
      val pos = fieldPos(propertyLikeMapping, PropertyMappingModel.Unique)
      result ++= Seq(MapEntryEmitter("unique", value.toString, YType.Bool, pos))
    }

    propertyLikeMapping.fields.entry(PropertyMappingModel.Pattern) foreach { entry =>
      val value = entry.value.value.asInstanceOf[AmfScalar].value.toString
      val pos   = fieldPos(propertyLikeMapping, entry.field)
      result ++= Seq(MapEntryEmitter("pattern", value, YType.Str, pos))
    }

    propertyLikeMapping.fields.entry(PropertyMappingModel.Minimum) foreach { entry =>
      val value = entry.value.value.asInstanceOf[AmfScalar].value
      val pos   = fieldPos(propertyLikeMapping, entry.field)
      result ++= Seq(MapEntryEmitter("minimum", value.toString, YType.Int, pos))
    }

    propertyLikeMapping.fields.entry(PropertyMappingModel.Maximum) foreach { entry =>
      val value = entry.value.value.asInstanceOf[AmfScalar].value
      val pos   = fieldPos(propertyLikeMapping, entry.field)
      result ++= Seq(MapEntryEmitter("maximum", value.toString, YType.Int, pos))
    }

    propertyLikeMapping.fields.entry(PropertyMappingModel.AllowMultiple) foreach { entry =>
      val value = entry.value.value.asInstanceOf[AmfScalar].value
      val pos   = fieldPos(propertyLikeMapping, entry.field)
      result ++= Seq(MapEntryEmitter("allowMultiple", value.toString, YType.Bool, pos))
    }

    propertyLikeMapping.fields.entry(PropertyMappingModel.Sorted) foreach { entry =>
      val value = entry.value.value.asInstanceOf[AmfScalar].value
      val pos   = fieldPos(propertyLikeMapping, entry.field)
      result ++= Seq(MapEntryEmitter("sorted", value.toString, YType.Bool, pos))
    }

    propertyLikeMapping.fields.entry(PropertyMappingModel.Enum) foreach { entry =>
      result ++= Seq(EnumEmitter(entry, ordering))
    }

    propertyLikeMapping.fields.entry(PropertyMappingModel.ExternallyLinkable) foreach { entry =>
      val value = entry.value.value.asInstanceOf[AmfScalar].value
      val pos   = fieldPos(propertyLikeMapping, entry.field)
      value match {
        case true  => result ++= Seq(MapEntryEmitter("isLink", "true", YType.Bool, pos))
        case false => result ++= Seq(MapEntryEmitter("isLink", "false", YType.Bool, pos))
      }
    }

    result ++= MandatoryEmitter(propertyLikeMapping).emitters()
    result ++= emitDiscriminator(propertyLikeMapping)
    result
  }
}

case class EnumEmitter(entry: FieldEntry, ordering: SpecOrdering) extends EntryEmitter {
  override def emit(b: EntryBuilder): Unit = {
    b.entry(
      "enum",
      _.list { b =>
        val scalars = emitters(entry.arrayValues)
        traverse(ordering.sorted(scalars), b)
      }
    )
  }

  private def emitters(values: Seq[Any]): Seq[ScalarEmitter] =
    values
      .filterType[AmfScalar]
      .map { scalar =>
        val tagType = scalar.value match {
          case _: Double  => YType.Float
          case _: Integer => YType.Int
          case _: Boolean => YType.Bool
          case _          => YType.Str
        }
        ScalarEmitter(scalar, tagType)
      }

  override def position(): Position = pos(entry.value.annotations)
}

case class MandatoryEmitter[T <: PropertyLikeMappingModel](propertyMapping: PropertyLikeMapping[T])
    extends PosExtractor {

  def emitters(): Seq[EntryEmitter] = {

    var emitters: Seq[EntryEmitter] = Seq()

    propertyMapping.mandatory().option() match {
      case Some(mandatory) =>
        val mandatoryPos = fieldPos(propertyMapping, PropertyMappingModel.Mandatory)
        propertyMapping.minCount().option() match {
          case Some(minCount) =>
            val minCountPos = fieldPos(propertyMapping, PropertyMappingModel.MinCount)
            emitters ++= Seq(
              MapEntryEmitter("minItems", minCount.toString, YType.Int, minCountPos),
              MapEntryEmitter("mandatory", mandatory.toString, YType.Bool, mandatoryPos)
            )
          case None => Seq(MapEntryEmitter("mandatory", mandatory.toString, YType.Bool, mandatoryPos))
        }
      case None =>
        propertyMapping.minCount().option() match {
          case Some(minCount) =>
            val minCountPos = fieldPos(propertyMapping, PropertyMappingModel.MinCount)
            emitters ++= Seq(MapEntryEmitter("mandatory", (minCount == 1).toString, YType.Bool, minCountPos))
          case None => // Nothing to do
        }
    }

    emitters
  }
}

case class PropertyMappingEmitter(
    dialect: Dialect,
    propertyMapping: PropertyMapping,
    ordering: SpecOrdering,
    aliases: Map[String, (String, String)]
)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with DiscriminatorEmitter
    with AliasesConsumer
    with PosExtractor
    with DefaultFacetEmission {
  override def emit(b: EntryBuilder): Unit = {
    b.entry(
      propertyMapping.name().value(),
      _.obj { b =>
        var emitters: Seq[EntryEmitter] =
          PropertyLikeMappingEmitter(dialect, propertyMapping, ordering, aliases).emitters

        propertyMapping.mergePolicy.option().foreach { policy =>
          val pos = fieldPos(propertyMapping, PropertyMappingModel.MergePolicy)
          emitters ++= Seq(
            new MapEntryEmitter("patch", policy, YType.Str, pos)
          )
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

        emitters ++= emitDefault(propertyMapping)

        ordering.sorted(emitters).foreach(_.emit(b))
      }
    )
  }

  private def position(annotations: Annotations): Position =
    annotations.find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)

  override def position(): Position = position(propertyMapping.annotations)
}
