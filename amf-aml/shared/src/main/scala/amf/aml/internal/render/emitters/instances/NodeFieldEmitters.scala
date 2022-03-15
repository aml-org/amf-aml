package amf.aml.internal.render.emitters.instances

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain._
import amf.aml.internal.metamodel.domain.{NodeMappableModel, PropertyLikeMappingModel}
import amf.aml.internal.registries.AMLRegistry
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfScalar, DomainElement}
import amf.core.internal.metamodel.Field
import amf.core.internal.parser.domain.{Annotations, FieldEntry, Value}
import amf.core.internal.render.BaseEmitters.{ArrayEmitter, EntryPartEmitter, ValueEmitter}
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import org.mulesoft.common.time.SimpleDateTime

object SemanticExtensionNodeEmitter {
  def apply(node: DomainElement,
            nodeMappable: NodeMappable[_ <: NodeMappableModel],
            dialect: Dialect,
            ordering: SpecOrdering,
            renderOptions: RenderOptions,
            registry: AMLRegistry,
            keyOverride: String)(implicit finder: NodeMappableFinder): NodeFieldEmitters = {
    NodeFieldEmitters(node,
                      nodeMappable,
                      dialect.references,
                      dialect,
                      ordering,
                      None,
                      None,
                      emitDialect = false,
                      topLevelEmitters = Nil,
                      renderOptions,
                      registry,
                      Some(keyOverride))
  }
}

case class NodeFieldEmitters(node: DomainElement,
                             nodeMappable: NodeMappable[_ <: NodeMappableModel],
                             references: Seq[BaseUnit],
                             dialect: Dialect,
                             ordering: SpecOrdering,
                             keyPropertyId: Option[String] = None,
                             discriminator: Option[(String, String)] = None,
                             emitDialect: Boolean = false,
                             topLevelEmitters: Seq[EntryEmitter] = Nil,
                             renderOptions: RenderOptions,
                             registry: AMLRegistry,
                             keyOverride: Option[String] = None)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends AmlEmittersHelper {

  def emitField(field: Field): Option[EntryEmitter] = {
    findPropertyMapping(field).filter { mapping =>
      keyPropertyId.isEmpty || mapping.nodePropertyMapping().value() != keyPropertyId.get
    } flatMap { propertyMapping =>
      val key                    = keyOverride.getOrElse(propertyMapping.name().value())
      val propertyClassification = propertyMapping.classification()
      node.fields
        .getValueAsOption(field)
        .map { value =>
          emitterFor(field, propertyMapping, key, propertyClassification, value)
        }
    }
  }

  private def emitterFor(field: Field,
                         propertyMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel],
                         key: String,
                         propertyClassification: PropertyClassification,
                         value: Value) = {
    (value.value, propertyClassification) match {
      case (scalar: AmfScalar, _) => emitScalar(key, field, scalar, Some(value.annotations))

      case (array: AmfArray, LiteralPropertyCollection) =>
        emitScalarArray(key, field, array, Some(value.annotations))

      case (element: DialectDomainElement, ExtensionPointProperty) => emitExternalObject(key, element)

      case (element: DialectDomainElement, ExternalLinkProperty) =>
        emitExternalLink(key, element, propertyMapping)

      case (element: DialectDomainElement, ObjectProperty) if !propertyMapping.isUnion =>
        emitObjectEntry(key, element, propertyMapping, Some(value.annotations))

      case (array: AmfArray, ExternalLinkProperty) =>
        emitExternalLink(key, array, propertyMapping, Some(value.annotations))

      case (array: AmfArray, ObjectPropertyCollection) if !propertyMapping.isUnion =>
        emitObjectEntry(key, array, propertyMapping, Some(value.annotations))

      case (array: AmfArray, ObjectMapProperty) =>
        emitObjectEntry(key, array, propertyMapping, Some(value.annotations))

      case (element: DialectDomainElement, ObjectProperty) if propertyMapping.isUnion =>
        emitObjectEntry(key, element, propertyMapping)

      case (array: AmfArray, ObjectPropertyCollection) if propertyMapping.isUnion =>
        emitObjectEntry(key, array, propertyMapping, Some(value.annotations))

      case (array: AmfArray, ObjectPairProperty) if propertyMapping.isInstanceOf[PropertyMapping] =>
        emitObjectPairs(key, array, propertyMapping.asInstanceOf[PropertyMapping], Some(value.annotations))
    }
  }

  protected def emitExternalLink(key: String,
                                 target: AmfElement,
                                 propertyMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel],
                                 annotations: Option[Annotations] = None): EntryEmitter = {

    ExternalLinkEmitter(key,
                        dialect,
                        target,
                        propertyMapping,
                        annotations,
                        keyPropertyId,
                        references,
                        ordering,
                        renderOptions,
                        registry)
  }

  private def emitScalar(key: String,
                         field: Field,
                         scalar: AmfScalar,
                         annotations: Option[Annotations] = None): EntryEmitter = {
    val formatted = scalar.value match {
      case date: SimpleDateTime => date.toString
      case other                => other
    }

    ValueEmitter(key, FieldEntry(field, Value(AmfScalar(formatted), annotations.getOrElse(scalar.annotations))))
  }

  private def emitScalarArray(key: String,
                              field: Field,
                              array: AmfArray,
                              annotations: Option[Annotations]): EntryEmitter =
    ArrayEmitter(key, FieldEntry(field, Value(array, annotations.getOrElse(array.annotations))), ordering)

  protected def emitObjectEntry(key: String,
                                target: AmfElement,
                                propertyMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel],
                                annotations: Option[Annotations] = None): EntryEmitter = {

    DialectObjectEntryEmitter(key,
                              target,
                              propertyMapping,
                              references,
                              dialect,
                              ordering,
                              renderOptions,
                              annotations,
                              registry)
  }

  protected def emitExternalObject(key: String, element: DialectDomainElement): EntryEmitter = {
    val (externalDialect, nextNodeMapping) = findNodeMappingById(element.definedBy.id)

    EntryPartEmitter(key,
                     DialectNodeEmitter(element,
                                        nextNodeMapping,
                                        references,
                                        externalDialect,
                                        ordering,
                                        emitDialect = true,
                                        renderOptions = renderOptions,
                                        registry = registry))
  }

  private def emitObjectPairs(key: String,
                              array: AmfArray,
                              propertyMapping: PropertyMapping,
                              annotations: Option[Annotations] = None): EntryEmitter = {
    ObjectPairEmitter(key, array, propertyMapping, annotations)
  }

  private def findPropertyMapping(field: Field): Option[PropertyLikeMapping[_ <: PropertyLikeMappingModel]] = {
    val iri = field.value.iri()
    nodeMappable match {
      case mapping: AnnotationMapping => Some(mapping)
      case nodeMapping: NodeMapping =>
        nodeMapping
          .propertiesMapping()
          .find(_.nodePropertyMapping().value() == iri)
      case unionMapping: UnionNodeMapping => checkRangeIds(unionMapping.objectRange().map(_.value()), iri)
      case conditionalMapping: ConditionalNodeMapping =>
        checkRangeIds(Seq(conditionalMapping.ifMapping.value(),
                          conditionalMapping.thenMapping.value(),
                          conditionalMapping.elseMapping.value()),
                      iri)

    }
  }
  private def checkRangeIds(rangeIds: Seq[String], iri: String): Option[PropertyMapping] = {
    val nodeMappingsInRange = rangeIds.map { id: String =>
      findNodeMappingById(id) match {
        case (_, nodeMapping: NodeMapping) => Some(nodeMapping)
        case _                             => None
      }
    } collect { case Some(nodeMapping: NodeMapping) => nodeMapping }
    // we need to do this because the same property (with different ranges) might be defined in multiple node mappings
    //        val nodeMetaTypes = node.meta.typeIri.map(_ -> true).toMap
    //        nodeMappingsInRange = nodeMappingsInRange.filter { nodeMapping => nodeMetaTypes.contains(nodeMapping.id) }
    nodeMappingsInRange.flatMap(_.propertiesMapping()).find(_.nodePropertyMapping().value() == iri)
  }
}
