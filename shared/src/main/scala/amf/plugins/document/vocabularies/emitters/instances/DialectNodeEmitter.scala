package amf.plugins.document.vocabularies.emitters.instances
import amf.core.annotations.Aliases.{Alias, ImportLocation, RefId}
import amf.core.annotations.LexicalInformation
import amf.core.emitter.BaseEmitters._
import amf.core.emitter.{EntryEmitter, PartEmitter, RenderOptions, SpecOrdering}
import amf.core.metamodel.Field
import amf.core.metamodel.domain.DomainElementModel
import amf.core.model.DataType
import amf.core.model.document.DeclaresModel
import amf.core.model.domain.extensions.DomainExtension
import amf.core.model.domain.{AmfArray, AmfElement, AmfScalar, ScalarNode}
import amf.core.parser.Position.ZERO
import amf.core.parser.{Annotations, FieldEntry, Position, Value}
import amf.plugins.document.vocabularies.annotations.{CustomId, JsonPointerRef, RefInclude}
import amf.plugins.document.vocabularies.metamodel.domain.DialectDomainElementModel
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceFragment, DialectInstanceUnit}
import amf.plugins.document.vocabularies.model.domain._
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.YDocument.{EntryBuilder, PartBuilder}
import org.yaml.model.{YNode, YType}

case class DialectNodeEmitter(node: DialectDomainElement,
                              nodeMappable: NodeMappable,
                              instance: DialectInstanceUnit,
                              dialect: Dialect,
                              ordering: SpecOrdering,
                              references: Map[RefId, (Alias, ImportLocation)],
                              keyPropertyId: Option[String] = None,
                              rootNode: Boolean = false,
                              discriminatorMappable: Option[NodeWithDiscriminator[Any]] = None,
                              discriminator: Option[(String, String)] = None,
                              emitDialect: Boolean = false,
                              topLevelEmitters: Seq[EntryEmitter] = Nil,
                              renderOptions: RenderOptions)
    extends PartEmitter
    with AmlEmittersHelper {

  override def emit(b: PartBuilder): Unit = {
    if (node.isLink) {
      if (isFragment(node, instance)) emitLink(node).emit(b)
      else if (isLibrary(node, instance)) {
        emitLibrarRef(node, instance, b)
      } else {
        emitRef(node, b)
      }
    } else {
      var emitters: Seq[EntryEmitter] = topLevelEmitters
      if (emitDialect) {
        emitters ++= Seq(MapEntryEmitter("$dialect", nodeMappable.id))
      }

      if (discriminator.isDefined) {
        val (discriminatorName, discriminatorValue) = discriminator.get
        emitters ++= Seq(MapEntryEmitter(discriminatorName, discriminatorValue))
      }
      if (node.annotations.find(classOf[CustomId]).isDefined || renderOptions.isEmitNodeIds) {
        val customId = if (node.id.contains(dialect.location().getOrElse(""))) {
          node.id.replace(dialect.id, "")
        } else {
          node.id
        }
        emitters ++= Seq(MapEntryEmitter("$id", customId))
      }

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
                        .isInstanceOf[DialectDomainElement] && propertyClassification == ObjectProperty && !propertyMapping.isUnion =>
                    val element = entry.value.asInstanceOf[DialectDomainElement]
                    val result  = emitObjectEntry(key, element, propertyMapping, Some(entry.annotations))
                    result

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

      // in case this is the root dialect node, we look for declarations
      if (rootNode)
        emitters ++= declarationsEmitters()

      // and also for use of libraries
      if (rootNode)
        emitters ++= Seq(ReferencesEmitter(instance, ordering, references))

      // finally emit the object
      b.obj { b =>
        ordering.sorted(emitters).foreach(_.emit(b))
      }
    }
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

  protected def emitLink(node: DialectDomainElement): PartEmitter =
    new PartEmitter {
      override def emit(b: PartBuilder): Unit = {
        if (node.annotations.contains(classOf[RefInclude])) {
          b.obj { m =>
            m.entry("$include", node.includeName)
          }
        } else if (node.annotations.contains(classOf[JsonPointerRef])) {
          b.obj { m =>
            m.entry("$ref", node.linkLabel.option().getOrElse(node.linkTarget.get.id))
          }
        } else {
          b += YNode.include(node.includeName)
        }
      }

      override def position(): Position =
        node.annotations
          .find(classOf[LexicalInformation])
          .map(_.range.start)
          .getOrElse(ZERO)
    }

  protected def emitRef(node: DialectDomainElement, b: PartBuilder): Unit = {
    if (node.annotations.contains(classOf[JsonPointerRef])) {
      b.obj { m =>
        m.entry("$ref", node.linkLabel.option().getOrElse(node.linkTarget.get.id))
      }
    } else {
      TextScalarEmitter(node.localRefName, node.annotations).emit(b)
    }
  }

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

  protected def findAllNodeMappings(mappableId: String): Seq[NodeMapping] = {
    findNodeMappingById(mappableId) match {
      case (_, nodeMapping: NodeMapping) => Seq(nodeMapping)
      case (_, unionMapping: UnionNodeMapping) =>
        val mappables = unionMapping.objectRange() map { rangeId =>
          findNodeMappingById(rangeId.value())._2
        }
        mappables.collect { case nodeMapping: NodeMapping => nodeMapping }
      case _ => Nil
    }
  }

  protected def emitObjectEntry(key: String,
                                target: AmfElement,
                                propertyMapping: PropertyMapping,
                                annotations: Option[Annotations] = None): Seq[EntryEmitter] = {
    // lets first extract the target values to emit, always as an array
    val elements: Seq[DialectDomainElement] = target match {
      case array: AmfArray =>
        array.values.asInstanceOf[Seq[DialectDomainElement]]
      case element: DialectDomainElement => Seq(element)
    }

    val isArray = target.isInstanceOf[AmfArray]
    val discriminator: DiscriminatorHelper =
      DiscriminatorHelper(propertyMapping, this)

    Seq(new EntryEmitter {
      // this can be multiple mappings if we have a union in the range or a range pointing to a union mapping
      val nodeMappings: Seq[NodeMapping] =
        propertyMapping.objectRange().flatMap { rangeNodeMapping =>
          findAllNodeMappings(rangeNodeMapping.value())
        }

      // val key property id, so we can pass it to the nested emitter and it is not emitted
      val keyPropertyId: Option[String] = propertyMapping.mapTermKeyProperty().option()

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
                    instance,
                    dialect,
                    ordering,
                    references,
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

    })
  }

  protected def emitExternalObject(key: String,
                                   element: DialectDomainElement,
                                   propertyMapping: PropertyMapping): Seq[EntryEmitter] = {
    val (externalDialect, nextNodeMapping) = findNodeMappingById(element.definedBy.id)
    Seq(
      EntryPartEmitter(key,
                       DialectNodeEmitter(element,
                                          nextNodeMapping,
                                          instance,
                                          externalDialect,
                                          ordering,
                                          references,
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

  def isFragment(elem: DialectDomainElement, instance: DialectInstanceUnit): Boolean = {
    elem.linkTarget match {
      case Some(domainElement) =>
        instance.references.exists {
          case ref: DialectInstanceFragment =>
            ref.encodes.id == domainElement.id
          case _ => false
        }
      case _ =>
        throw new Exception(s"Cannot check fragment for an element without target for element ${elem.id}")
    }
  }

  def isLibrary(elem: DialectDomainElement, instance: DialectInstanceUnit): Boolean = {
    instance.references.exists {
      case lib: DeclaresModel =>
        lib.declares.exists(_.id == elem.linkTarget.get.id)
      case _ => false
    }
  }

  def emitLibrarRef(elem: DialectDomainElement, instance: DialectInstanceUnit, b: PartBuilder): Unit = {
    if (elem.annotations.contains(classOf[JsonPointerRef])) {
      b.obj { m =>
        m.entry("$ref", node.linkLabel.option().getOrElse(node.linkTarget.get.id))
      }
    } else {
      val lib = instance.references.find {
        case lib: DeclaresModel =>
          lib.declares.exists(_.id == elem.linkTarget.get.id)
        case _ => false
      }
      val alias = references(lib.get.id)._1
      TextScalarEmitter(s"$alias.${elem.localRefName}", elem.annotations)
        .emit(b)
    }
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
                      references,
                      renderOptions = renderOptions
                    ))
              }
            } else acc
        }
      }
    }
    emitters.getOrElse(Nil)
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
