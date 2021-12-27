package amf.aml.internal.render.emitters.instances

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain._
import amf.aml.internal.annotations.{CustomBase, CustomId}
import amf.aml.internal.metamodel.domain.{DialectDomainElementModel, NodeMappableModel}
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfScalar}
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.DomainElementModel
import amf.core.internal.parser.domain.{Annotations, FieldEntry, Value}
import amf.core.internal.render.BaseEmitters.{ArrayEmitter, EntryPartEmitter, ValueEmitter}
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.YDocument.PartBuilder

case class NodeFieldEmitters(node: DialectDomainElement,
                             nodeMappable: NodeMappable[_ <: NodeMappableModel],
                             references: Seq[BaseUnit],
                             dialect: Dialect,
                             ordering: SpecOrdering,
                             keyPropertyId: Option[String] = None,
                             discriminator: Option[(String, String)] = None,
                             emitDialect: Boolean = false,
                             topLevelEmitters: Seq[EntryEmitter] = Nil,
                             renderOptions: RenderOptions)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends AmlEmittersHelper {

  def emitField(field: Field): Seq[EntryEmitter] = {
    findPropertyMapping(field) map { propertyMapping =>
      if (keyPropertyId.isEmpty || propertyMapping
            .nodePropertyMapping()
            .value() != keyPropertyId.get) {

        val key                    = propertyMapping.name().value()
        val propertyClassification = propertyMapping.classification()

        val nextEmitter: Seq[EntryEmitter] =
          node.fields.getValueAsOption(field) match {
            case Some(entry) if isScalar(entry) =>
              val scalar = entry.value.asInstanceOf[AmfScalar]
              emitScalar(key, field, scalar, Some(entry.annotations))

            case Some(entry) if isArray(entry) && propertyClassification == LiteralPropertyCollection =>
              val array = entry.value.asInstanceOf[AmfArray]
              emitScalarArray(key, field, array, Some(entry.annotations))

            case Some(entry)
                if entry.value
                  .isInstanceOf[DialectDomainElement] && propertyClassification == ExtensionPointProperty =>
              val element = entry.value.asInstanceOf[DialectDomainElement]
              emitExternalObject(key, element, propertyMapping)

            case Some(entry)
                if entry.value
                  .isInstanceOf[DialectDomainElement] && propertyClassification == ExternalLinkProperty =>
              val element = entry.value.asInstanceOf[DialectDomainElement]
              emitExternalLink(key, element, propertyMapping)

            case Some(entry)
                if entry.value
                  .isInstanceOf[DialectDomainElement] && propertyClassification == ObjectProperty && !propertyMapping.isUnion =>
              val element = entry.value.asInstanceOf[DialectDomainElement]
              val result  = emitObjectEntry(key, element, propertyMapping, Some(entry.annotations))
              result

            case Some(entry) if isArray(entry) && propertyClassification == ExternalLinkProperty =>
              val array = entry.value.asInstanceOf[AmfArray]
              emitExternalLink(key, array, propertyMapping, Some(entry.annotations))

            case Some(entry)
                if isArray(entry) && propertyClassification == ObjectPropertyCollection && !propertyMapping.isUnion =>
              val array = entry.value.asInstanceOf[AmfArray]
              emitObjectEntry(key, array, propertyMapping, Some(entry.annotations))

            case Some(entry) if isArray(entry) && propertyClassification == ObjectMapProperty =>
              val array = entry.value.asInstanceOf[AmfArray]
              emitObjectEntry(key, array, propertyMapping, Some(entry.annotations))

            case Some(entry)
                if entry.value
                  .isInstanceOf[DialectDomainElement] && propertyClassification == ObjectProperty && propertyMapping.isUnion =>
              val element = entry.value.asInstanceOf[DialectDomainElement]
              emitObjectEntry(key, element, propertyMapping)

            case Some(entry)
                if isArray(entry) && propertyClassification == ObjectPropertyCollection && propertyMapping.isUnion =>
              val array = entry.value.asInstanceOf[AmfArray]
              emitObjectEntry(key, array, propertyMapping, Some(entry.annotations))

            case Some(entry) if isArray(entry) && propertyClassification == ObjectPairProperty =>
              val array = entry.value.asInstanceOf[AmfArray]
              emitObjectPairs(key, array, propertyMapping, Some(entry.annotations))
            case None => Nil // ignore
          }
        nextEmitter
      } else Nil
    }
  }.getOrElse(Nil)

  private def isArray(entry: Value) = {
    entry.value
      .isInstanceOf[AmfArray]
  }

  private def isScalar(entry: Value) = {
    entry.value.isInstanceOf[AmfScalar]
  }

  protected def emitExternalLink(key: String,
                                 target: AmfElement,
                                 propertyMapping: PropertyMapping,
                                 annotations: Option[Annotations] = None): Seq[EntryEmitter] = {
    Seq(
        ExternalLinkEmitter(key,
                            dialect,
                            target,
                            propertyMapping,
                            annotations,
                            keyPropertyId,
                            references,
                            ordering,
                            renderOptions))
  }

  private def nodeMappingForObjectProperty(propertyMapping: PropertyMapping,
                                           dialectDomainElement: DialectDomainElement): Option[NodeMappable] = {
    // this can be multiple mappings if we have a union in the range or a range pointing to a union mapping
    val nodeMappings: Seq[NodeMapping] =
      propertyMapping.objectRange().flatMap { rangeNodeMapping =>
        findAllNodeMappings(rangeNodeMapping.value())
      }
    nodeMappings.find(
        nodeMapping =>
          dialectDomainElement.meta.`type`
            .map(_.iri())
            .exists(i => i == nodeMapping.nodetypeMapping.value() || i == nodeMapping.id))
  }

  private def emitScalar(key: String,
                         field: Field,
                         scalar: AmfScalar,
                         annotations: Option[Annotations] = None): Seq[EntryEmitter] = {
    val formatted = scalar.value match {
      case date: SimpleDateTime => date.toString
      case other                => other
    }

    Seq(ValueEmitter(key, FieldEntry(field, Value(AmfScalar(formatted), annotations.getOrElse(scalar.annotations)))))
  }

  private def emitScalarArray(key: String,
                              field: Field,
                              array: AmfArray,
                              annotations: Option[Annotations]): Seq[EntryEmitter] =
    Seq(ArrayEmitter(key, FieldEntry(field, Value(array, annotations.getOrElse(array.annotations))), ordering))

  protected def emitObjectEntry(key: String,
                                target: AmfElement,
                                propertyMapping: PropertyMapping,
                                annotations: Option[Annotations] = None): Seq[EntryEmitter] = {

    Seq(
        DialectObjectEntryEmitter(key,
                                  target,
                                  propertyMapping,
                                  references,
                                  dialect,
                                  ordering,
                                  renderOptions,
                                  annotations))
  }

  protected def emitExternalObject(key: String,
                                   element: DialectDomainElement,
                                   propertyMapping: PropertyMapping): Seq[EntryEmitter] = {
    val (externalDialect, nextNodeMapping) = findNodeMappingById(element.definedBy.id)
    Seq(
        EntryPartEmitter(key,
                         DialectNodeEmitter(element,
                                            nextNodeMapping,
                                            references,
                                            externalDialect,
                                            ordering,
                                            emitDialect = true,
                                            renderOptions = renderOptions)))
  }

  private def emitObjectPairs(key: String,
                              array: AmfArray,
                              propertyMapping: PropertyMapping,
                              annotations: Option[Annotations] = None): Seq[EntryEmitter] = {
    Seq(ObjectPairEmitter(key, array, propertyMapping, annotations))
  }

  private def findPropertyMapping(field: Field): Option[PropertyMapping] = {
    val iri = field.value.iri()
    nodeMappable match {
      case nodeMapping: NodeMapping =>
        nodeMapping
          .propertiesMapping()
          .find(_.nodePropertyMapping().value() == iri)
      case unionMapping: UnionNodeMapping =>
        val rangeIds: Seq[String] = unionMapping.objectRange().map(_.value())
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
}
