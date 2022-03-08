package amf.aml.internal.render.emitters.instances

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping, PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.metamodel.domain.PropertyLikeMappingModel
import amf.aml.internal.registries.AMLRegistry
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfElement}
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.render.BaseEmitters.EntryPartEmitter
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import org.yaml.model.YDocument.EntryBuilder

private case class DialectObjectEntryEmitter[M <: PropertyLikeMappingModel](
    key: String,
    target: AmfElement,
    propertyMapping: PropertyLikeMapping[M],
    references: Seq[BaseUnit],
    dialect: Dialect,
    ordering: SpecOrdering,
    renderOptions: RenderOptions,
    annotations: Option[Annotations] = None,
    registry: AMLRegistry)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AmlEmittersHelper {

  // this can be multiple mappings if we have a union in the range or a range pointing to a union mapping
  val nodeMappings: Seq[NodeMapping] = propertyMapping.objectRange().flatMap { rangeNodeMapping =>
    findAllNodeMappings(rangeNodeMapping.value())
  }

  // val key property id, so we can pass it to the nested emitter and it is not emitted
  val keyPropertyId: Option[String] = propertyMapping match {
    case mapping: PropertyMapping => mapping.mapTermKeyProperty().option()
    case _                        => None
  }

  // lets first extract the target values to emit, always as an array
  val elements: Seq[DialectDomainElement] = target match {
    case array: AmfArray =>
      array.values.asInstanceOf[Seq[DialectDomainElement]]
    case element: DialectDomainElement => Seq(element)
  }

  val isArray: Boolean = target.isInstanceOf[AmfArray]
  val discriminator: DiscriminatorHelper =
    DiscriminatorHelper(propertyMapping, this)

  override def emit(b: EntryBuilder): Unit = {
    // collect the emitters for each element, based on the available mappings
    val mappedElements: Map[DialectNodeEmitter, DialectDomainElement] =
      elements.foldLeft(Map[DialectNodeEmitter, DialectDomainElement]()) {
        case (acc, dialectDomainElement: DialectDomainElement) =>
          // Let's see if this element has a discriminator to add
          nodeMappings.find(
              nodeMapping =>
                dialectDomainElement.meta.`type`
                  .map(_.iri())
                  .exists(i => i == nodeMapping.nodetypeMapping.value() || i == nodeMapping.id)) match {
            case Some(nextNodeMapping) =>
              val nodeEmitter = DialectNodeEmitter(
                  dialectDomainElement,
                  nextNodeMapping,
                  references,
                  dialect,
                  ordering,
                  discriminator = discriminator.compute(dialectDomainElement),
                  keyPropertyId = keyPropertyId,
                  renderOptions = renderOptions,
                  registry = registry
              )
              acc + (nodeEmitter -> dialectDomainElement)
            case _ =>
              acc // TODO: raise violation
          }
        case (acc, _) => acc
      }

    if (keyPropertyId.isDefined) {
      // emit map of nested objects by property
      emitMap(b, mappedElements, keyPropertyId.get)
    } else if (isArray) {
      // arrays of objects
      emitArray(b, mappedElements)
    } else {
      // single object
      emitSingleElement(b, mappedElements)
    }
  }

  private def emitMap(b: EntryBuilder,
                      mapElements: Map[DialectNodeEmitter, DialectDomainElement],
                      keyPropertyIdValue: String): Unit = {
    b.entry(
        key,
        _.obj { b =>
          ordering.sorted(mapElements.keys.toSeq).foreach { emitter =>
            val dialectDomainElement = mapElements(emitter)
            val mapKeyField =
              dialectDomainElement.meta.fields
                .find(_.value.iri() == keyPropertyIdValue)
                .get
            val mapKeyValue =
              dialectDomainElement.fields.getValue(mapKeyField).toString
            EntryPartEmitter(mapKeyValue, emitter).emit(b)
          }
        }
    )
  }

  def emitArray(b: EntryBuilder, mappedElements: Map[DialectNodeEmitter, DialectDomainElement]): Unit = {
    b.entry(key, _.list { b =>
      ordering.sorted(mappedElements.keys.toSeq).foreach(_.emit(b))
    })
  }

  def emitSingleElement(b: EntryBuilder, mappedElements: Map[DialectNodeEmitter, DialectDomainElement]): Unit = {
    mappedElements.keys.headOption.foreach { emitter =>
      EntryPartEmitter(key, emitter).emit(b)
    }
  }

  override def position(): Position = {

    annotations
      .flatMap(_.find(classOf[LexicalInformation]))
      .orElse(target.annotations.find(classOf[LexicalInformation]))
      .map(_.range.start)
      .getOrElse(ZERO)
  }

}
