package amf.plugins.document.vocabularies.emitters.instances
import amf.client.remod.amfcore.config.RenderOptions
import amf.core.annotations.Aliases.{Alias, ImportLocation, RefId}
import amf.core.annotations.{LexicalInformation, SourceNode}
import amf.core.emitter.BaseEmitters._
import amf.core.emitter.{EntryEmitter, PartEmitter, SpecOrdering}
import amf.core.metamodel.Field
import amf.core.metamodel.domain.DomainElementModel
import amf.core.model.DataType
import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.model.domain.extensions.DomainExtension
import amf.core.model.domain.{AmfArray, AmfElement, AmfScalar, ScalarNode}
import amf.core.parser.Position.ZERO
import amf.core.parser.{Annotations, FieldEntry, Position, Value}
import amf.plugins.document.vocabularies.annotations.{CustomBase, CustomId, JsonPointerRef, RefInclude}
import amf.plugins.document.vocabularies.metamodel.domain.{DialectDomainElementModel, NodeMappableModel}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceFragment, DialectInstanceUnit}
import amf.plugins.document.vocabularies.model.domain._
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.YDocument.{EntryBuilder, PartBuilder}
import org.yaml.model.{YNode, YType}

import scala.language.existentials

class RootDialectNodeEmitter(node: DialectDomainElement,
                             nodeMappable: NodeMappable[_ <: NodeMappableModel],
                             instance: DialectInstanceUnit,
                             dialect: Dialect,
                             ordering: SpecOrdering,
                             keyPropertyId: Option[String] = None,
                             discriminator: Option[(String, String)] = None,
                             emitDialect: Boolean = false,
                             topLevelEmitters: Seq[EntryEmitter] = Nil,
                             renderOptions: RenderOptions)
    extends DialectNodeEmitter(node,
                               nodeMappable,
                               instance.references,
                               dialect,
                               ordering,
                               keyPropertyId,
                               discriminator,
                               emitDialect,
                               topLevelEmitters,
                               renderOptions) {

  lazy val referencesAliasIndex: Map[RefId, (Alias, ImportLocation)] = buildReferenceAliasIndexFrom(instance)

  override def emitters: Seq[EntryEmitter] = {
    var emitters = super.emitters
    // in case this is the root dialect node, we look for declarations
    emitters ++= declarationsEmitters()

    // and also for use of libraries
    emitters ++= Seq(ReferencesEmitter(instance, ordering, referencesAliasIndex))
    emitters
  }

  private def declares(): Option[DeclaresModel] = instance match {
    case d: DeclaresModel => Some(d)
    case _                => None
  }

  def declarationsEmitters(): Seq[EntryEmitter] = {
    val emitters = for {
      docs  <- Option(dialect.documents())
      root  <- Option(docs.root())
      model <- declares()
    } yield {
      if (root.encoded().value() == node.id) {
        Nil
      } else {
        root.declaredNodes().foldLeft(Seq[EntryEmitter]()) {
          case (acc, publicNodeMapping) =>
            val publicMappings = findAllNodeMappings(publicNodeMapping.mappedNode().value()).map(_.id).toSet
            val declared = model.declares.collect {
              case elem: DialectDomainElement if publicMappings.contains(elem.definedBy.id) => elem
            }
            if (declared.nonEmpty) {
              findNodeMappingById(publicNodeMapping.mappedNode().value()) match {
                case (_, nodeMappable: NodeMappable) =>
                  acc ++ Seq(
                      DeclarationsGroupEmitter(
                          declared,
                          publicNodeMapping,
                          nodeMappable,
                          instance,
                          dialect,
                          ordering,
                          docs
                            .declarationsPath()
                            .option()
                            .getOrElse("/")
                            .split("/"),
                          referencesAliasIndex,
                          renderOptions = renderOptions
                      ))
              }
            } else acc
        }
      }
    }
    emitters.getOrElse(Nil)
  }
}

case class DialectNodeEmitter(node: DialectDomainElement,
                              nodeMappable: NodeMappable[_ <: NodeMappableModel],
                              references: Seq[BaseUnit],
                              dialect: Dialect,
                              ordering: SpecOrdering,
                              keyPropertyId: Option[String] = None,
                              discriminator: Option[(String, String)] = None,
                              emitDialect: Boolean = false,
                              topLevelEmitters: Seq[EntryEmitter] = Nil,
                              renderOptions: RenderOptions)
    extends PartEmitter
    with AmlEmittersHelper {

  override def emit(b: PartBuilder): Unit = {
    if (node.isLink)
      DialectDomainElementLinkEmitter(node, references).emit(b)
    else
      b.obj { b =>
        ordering.sorted(emitters).foreach(_.emit(b))
      }
  }

  def emitters: Seq[EntryEmitter] = {
    var emitters: Seq[EntryEmitter] = topLevelEmitters
    if (emitDialect) {
      emitters ++= Seq(MapEntryEmitter("$dialect", nodeMappable.id))
    }

    if (discriminator.isDefined) {
      val (discriminatorName, discriminatorValue) = discriminator.get
      emitters ++= Seq(MapEntryEmitter(discriminatorName, discriminatorValue))
    }
    if (node.annotations.find(classOf[CustomId]).isDefined || renderOptions.isEmitNodeIds) {
      val baseId = node.annotations.find(classOf[CustomId]) match {
        case Some(customId) if customId.value != "true" => customId.value
        case _                                          => node.id
      }
      val customId = if (baseId.contains(dialect.location().getOrElse(""))) {
        baseId.replace(dialect.id, "")
      } else {
        baseId
      }
      emitters ++= Seq(MapEntryEmitter("$id", customId))
    }

    if (node.annotations.find(classOf[CustomBase]).isDefined) {
      node.annotations.find(classOf[CustomBase]) match {
        case Some(customBase) if customBase.value != "true" =>
          emitters ++= Seq(MapEntryEmitter("$base", customBase.value))
        case _ => // Nothing
      }
    }

    emitters ++= fieldAndExtensionEmitters
    emitters
  }

  private def fieldAndExtensionEmitters: Seq[EntryEmitter] = {
    var emitters: Seq[EntryEmitter] = Nil
    uniqueFields(node.meta).foreach {
      case DomainElementModel.CustomDomainProperties =>
        node.fields.get(DomainElementModel.CustomDomainProperties) match {
          case AmfArray(customDomainProperties, _) =>
            customDomainProperties.foreach {
              case domainExtension: DomainExtension =>
                val extensionName = domainExtension.name.value()
                domainExtension.`extension` match {
                  case s: ScalarNode =>
                    val extensionValue = s.value.value()
                    val tagType = s.dataType.value() match {
                      case DataType.Integer  => YType.Int
                      case DataType.Float    => YType.Float
                      case DataType.Boolean  => YType.Bool
                      case DataType.Nil      => YType.Null
                      case DataType.DateTime => YType.Timestamp
                      case _                 => YType.Str
                    }
                    val position = domainExtension.annotations
                      .find(classOf[LexicalInformation])
                      .map(_.range.start)
                      .getOrElse(Position.ZERO)
                    emitters ++= Seq(
                        MapEntryEmitter(extensionName, extensionValue, tag = tagType, position = position))
                  case _ => // Ignore
                }

            }
          case _ => // Ignore
        }

      case field =>
        findPropertyMapping(node, field) foreach { propertyMapping =>
          if (keyPropertyId.isEmpty || propertyMapping
                .nodePropertyMapping()
                .value() != keyPropertyId.get) {

            val key                    = propertyMapping.name().value()
            val propertyClassification = propertyMapping.classification()

            val nextEmitter: Seq[EntryEmitter] =
              node.fields.getValueAsOption(field) match {
                case Some(entry) if entry.value.isInstanceOf[AmfScalar] =>
                  val scalar = entry.value.asInstanceOf[AmfScalar]
                  emitScalar(key, field, scalar, Some(entry.annotations))

                case Some(entry)
                    if entry.value
                      .isInstanceOf[AmfArray] && propertyClassification == LiteralPropertyCollection =>
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

                case Some(entry)
                    if entry.value
                      .isInstanceOf[AmfArray] && propertyClassification == ExternalLinkProperty =>
                  val array = entry.value.asInstanceOf[AmfArray]
                  emitExternalLink(key, array, propertyMapping, Some(entry.annotations))

                case Some(entry)
                    if entry.value
                      .isInstanceOf[AmfArray] && propertyClassification == ObjectPropertyCollection && !propertyMapping.isUnion =>
                  val array = entry.value.asInstanceOf[AmfArray]
                  emitObjectEntry(key, array, propertyMapping, Some(entry.annotations))

                case Some(entry)
                    if entry.value
                      .isInstanceOf[AmfArray] && propertyClassification == ObjectMapProperty =>
                  val array = entry.value.asInstanceOf[AmfArray]
                  emitObjectEntry(key, array, propertyMapping, Some(entry.annotations))

                case Some(entry)
                    if entry.value
                      .isInstanceOf[DialectDomainElement] && propertyClassification == ObjectProperty && propertyMapping.isUnion =>
                  val element = entry.value.asInstanceOf[DialectDomainElement]
                  emitObjectEntry(key, element, propertyMapping)

                case Some(entry)
                    if entry.value
                      .isInstanceOf[AmfArray] && propertyClassification == ObjectPropertyCollection && propertyMapping.isUnion =>
                  val array = entry.value.asInstanceOf[AmfArray]
                  emitObjectEntry(key, array, propertyMapping, Some(entry.annotations))

                case Some(entry)
                    if entry.value
                      .isInstanceOf[AmfArray] && propertyClassification == ObjectPairProperty =>
                  val array = entry.value.asInstanceOf[AmfArray]
                  emitObjectPairs(key, array, propertyMapping, Some(entry.annotations))
                case None => Nil // ignore
              }
            emitters ++= nextEmitter
          }
        }
    }
    emitters
  }

  protected def emitExternalLink(key: String,
                                 target: AmfElement,
                                 propertyMapping: PropertyMapping,
                                 annotations: Option[Annotations] = None): Seq[EntryEmitter] = {
    Seq(new EntryEmitter {
      override def emit(b: EntryBuilder): Unit = {
        b.entry(
            key,
            (e) => {
              target match {
                case array: AmfArray =>
                  e.list(l => {
                    array.values.asInstanceOf[Seq[DialectDomainElement]].foreach {
                      elem =>
                        if (elem.fields.nonEmpty) { // map reference
                          nodeMappingForObjectProperty(propertyMapping, elem) match {
                            case Some(rangeMapping) =>
                              DialectNodeEmitter(
                                  elem,
                                  rangeMapping,
                                  references,
                                  dialect,
                                  ordering,
                                  discriminator = None,
                                  keyPropertyId = keyPropertyId,
                                  renderOptions = renderOptions
                              ).emit(l)
                            case _ => // ignore, error
                          }
                        } else { // just link
                          emitCustomId(elem, l)
                          emitCustomBase(elem, l)
                        }
                    }
                  })
                case element: DialectDomainElement =>
                  emitCustomId(element, e)
                  emitCustomBase(element, e)
              }
            }
        )
      }

      override def position(): Position = {
        annotations
          .flatMap(_.find(classOf[LexicalInformation]))
          .orElse(target.annotations.find(classOf[LexicalInformation]))
          .map(_.range.start)
          .getOrElse(ZERO)
      }
    })
  }

  def emitCustomId(elem: DialectDomainElement, b: PartBuilder): Unit = {
    elem.annotations.find(classOf[CustomId]) match {
      case Some(customId) if customId.value != "true" =>
        b.obj { m =>
          m.entry("$id", customId.value)
        }
      case Some(_) =>
        b.obj { m =>
          m.entry("$id", elem.id)
        }
      case _ => b += elem.id
    }
  }

  def emitCustomBase(elem: DialectDomainElement, b: PartBuilder): Unit = {
    elem.annotations.find(classOf[CustomBase]) match {
      case Some(customBase) if customBase.value != "true" =>
        b.obj { m =>
          m.entry("$base", customBase.value)
        }
      case _ => // Nothing
    }
  }

  protected def nodeMappingForObjectProperty(propertyMapping: PropertyMapping,
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

  protected def uniqueFields(meta: DialectDomainElementModel): Iterable[Field] = {
    val allFields = meta.fields :+ DomainElementModel.CustomDomainProperties
    var acc       = Map[String, Field]()
    allFields.foreach { f =>
      acc.get(f.value.iri()) match {
        case Some(_) => // ignore
        case _ =>
          acc = acc.updated(f.value.iri(), f)
      }
    }
    acc.values
  }
  override def position(): Position =
    node.annotations
      .find(classOf[LexicalInformation])
      .map(_.range.start)
      .getOrElse(ZERO)

  protected def emitScalar(key: String,
                           field: Field,
                           scalar: AmfScalar,
                           annotations: Option[Annotations] = None): Seq[EntryEmitter] = {
    val formatted = scalar.value match {
      case date: SimpleDateTime => date.toString
      case other                => other
    }

    Seq(ValueEmitter(key, FieldEntry(field, Value(AmfScalar(formatted), annotations.getOrElse(scalar.annotations)))))
  }

  protected def emitScalarArray(key: String,
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

  protected def emitObjectPairs(key: String,
                                array: AmfArray,
                                propertyMapping: PropertyMapping,
                                annotations: Option[Annotations] = None): Seq[EntryEmitter] = {
    val keyProperty   = propertyMapping.mapTermKeyProperty().value()
    val valueProperty = propertyMapping.mapTermValueProperty().value()

    Seq(new EntryEmitter() {
      override def emit(b: EntryBuilder): Unit = {
        b.entry(
            key,
            _.obj {
              b =>
                val sortedElements = array.values.sortBy { elem =>
                  elem.annotations
                    .find(classOf[LexicalInformation])
                    .map(_.range.start)
                    .getOrElse(ZERO)
                }
                sortedElements.foreach {
                  case element: DialectDomainElement =>
                    val keyField =
                      element.meta.fields.find(_.value.iri() == keyProperty)
                    val valueField =
                      element.meta.fields.find(_.value.iri() == valueProperty)
                    if (keyField.isDefined && valueField.isDefined) {
                      val keyLiteral =
                        element.fields.getValueAsOption(keyField.get).map(_.value)
                      val valueLiteral = element.fields
                        .getValueAsOption(valueField.get)
                        .map(_.value)
                      (keyLiteral, valueLiteral) match {
                        case (Some(keyScalar: AmfScalar), Some(valueScalar: AmfScalar)) =>
                          MapEntryEmitter(keyScalar.value.toString, valueScalar.value.toString).emit(b)
                        case _ =>
                          throw new Exception("Cannot generate object pair without scalar values for key and value")
                      }
                    } else {
                      throw new Exception("Cannot generate object pair with undefined key or value")
                    }
                  case _ => // ignore
                }
            }
        )
      }

      override def position(): Position =
        annotations
          .flatMap(_.find(classOf[LexicalInformation]))
          .orElse(array.annotations.find(classOf[LexicalInformation]))
          .map(_.range.start)
          .getOrElse(ZERO)
    })
  }

  protected def findPropertyMapping(node: DialectDomainElement, field: Field): Option[PropertyMapping] = {
    val iri = field.value.iri()
    nodeMappable match {
      case nodeMapping: NodeMapping =>
        nodeMapping
          .propertiesMapping()
          .find(_.nodePropertyMapping().value() == iri)
      case unionMapping: UnionNodeMapping =>
        val rangeIds: Seq[String] = unionMapping.objectRange().map(_.value())
        var nodeMappingsInRange = rangeIds.map { id: String =>
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

case class DialectDomainElementLinkEmitter(node: DialectDomainElement, references: Seq[BaseUnit]) extends PartEmitter {
  override def emit(b: PartBuilder): Unit = {
    if (node.annotations.contains(classOf[RefInclude])) {
      b.obj { m =>
        m.entry("$include", node.includeName)
      }
    } else if (node.annotations.contains(classOf[JsonPointerRef])) {
      b.obj { m =>
        m.entry("$ref", node.linkLabel.option().getOrElse(node.linkTarget.get.id))
      }
    } else if (isFragmentRef(node, references)) {
      b += YNode.include(node.includeName)
    } else {
      // case of library and declaration references
      TextScalarEmitter(node.linkLabel.value(), node.annotations).emit(b)
    }
  }

  def isFragmentRef(elem: DialectDomainElement, references: Seq[BaseUnit]): Boolean = {
    elem.annotations.find(classOf[SourceNode]) match {
      case Some(SourceNode(node)) => node.tagType == YType.Include
      case None if references.nonEmpty =>
        elem.linkTarget match {
          case Some(domainElement) =>
            references.exists {
              case ref: DialectInstanceFragment =>
                ref.encodes.id == domainElement.id
              case _ => false
            }
          case _ =>
            throw new Exception(s"Cannot check fragment for an element without target for element ${elem.id}")
        }
      case _ => false
    }
  }

  override def position(): Position =
    node.annotations
      .find(classOf[LexicalInformation])
      .map(_.range.start)
      .getOrElse(ZERO)
}

private case class DialectObjectEntryEmitter(key: String,
                                             target: AmfElement,
                                             propertyMapping: PropertyMapping,
                                             references: Seq[BaseUnit],
                                             dialect: Dialect,
                                             ordering: SpecOrdering,
                                             renderOptions: RenderOptions,
                                             annotations: Option[Annotations] = None)
    extends EntryEmitter
    with AmlEmittersHelper {
  // this can be multiple mappings if we have a union in the range or a range pointing to a union mapping
  val nodeMappings: Seq[NodeMapping] =
    propertyMapping.objectRange().flatMap { rangeNodeMapping =>
      findAllNodeMappings(rangeNodeMapping.value())
    }

  // val key property id, so we can pass it to the nested emitter and it is not emitted
  val keyPropertyId: Option[String] = propertyMapping.mapTermKeyProperty().option()

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
                  .contains(nodeMapping.nodetypeMapping.value())) match {
            case Some(nextNodeMapping) =>
              val nodeEmitter = DialectNodeEmitter(
                  dialectDomainElement,
                  nextNodeMapping,
                  references,
                  dialect,
                  ordering,
                  discriminator = discriminator.compute(dialectDomainElement),
                  keyPropertyId = keyPropertyId,
                  renderOptions = renderOptions
              )
              acc + (nodeEmitter -> dialectDomainElement)
            case _ =>
              acc // TODO: raise violation
          }
        case (acc, _) => acc
      }

    if (keyPropertyId.isDefined) {
      // emit map of nested objects by property
      emitMap(b, mappedElements)
    } else if (isArray) {
      // arrays of objects
      emitArray(b, mappedElements)
    } else {
      // single object
      emitSingleElement(b, mappedElements)
    }
  }

  def emitMap(b: EntryBuilder, mapElements: Map[DialectNodeEmitter, DialectDomainElement]): Unit = {
    b.entry(
        key,
        _.obj { b =>
          ordering.sorted(mapElements.keys.toSeq).foreach { emitter =>
            val dialectDomainElement = mapElements(emitter)
            val mapKeyField =
              dialectDomainElement.meta.fields
                .find(_.value
                  .iri() == propertyMapping.mapTermKeyProperty().value())
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
