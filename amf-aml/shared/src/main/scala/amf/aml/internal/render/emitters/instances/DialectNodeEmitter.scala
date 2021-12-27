package amf.aml.internal.render.emitters.instances
import amf.core.client.common.position.Position
import amf.core.client.scala.config.RenderOptions
import amf.core.internal.annotations.Aliases.{Alias, ImportLocation, RefId}
import amf.core.internal.annotations.{LexicalInformation, SourceNode}
import amf.core.internal.render.BaseEmitters._
import amf.core.internal.render.emitters.{EntryEmitter, PartEmitter}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.domain.DomainElementModel
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfScalar, ScalarNode}
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.internal.parser.domain.{Annotations, FieldEntry, Value}
import amf.core.internal.render.SpecOrdering
import amf.aml.internal.annotations.{CustomBase, CustomId, JsonPointerRef, RefInclude}
import amf.aml.internal.metamodel.domain.{DialectDomainElementModel, NodeMappableModel}
import amf.aml.client.scala.model.document.{Dialect, DialectInstanceFragment, DialectInstanceUnit}
import amf.aml.client.scala.model.domain._
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
                             renderOptions: RenderOptions)(implicit nodeMappableFinder: NodeMappableFinder)
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
                              renderOptions: RenderOptions)(implicit val nodeMappableFinder: NodeMappableFinder)
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
    if (emitDialect) emitters ++= Seq(MapEntryEmitter("$dialect", nodeMappable.id))
    emitters ++= emitDiscriminator()
    emitters ++= emitId()
    emitters ++= emitCustomBase()
    emitters ++= fieldAndExtensionEmitters
    emitters
  }

  private def emitCustomBase(): Seq[EntryEmitter] = {
    customBaseOf(node)
      .filter(_.value != "true")
      .map(base => Seq(MapEntryEmitter("$base", base.value)))
      .getOrElse(Nil)
  }

  private def customBaseOf(node: DialectDomainElement) = node.annotations.find(classOf[CustomBase])

  private def emitId(): Seq[EntryEmitter] = {
    if (hasCustomId(node) || renderOptions.isEmitNodeIds) {
      val baseId = customIdOf(node) match {
        case Some(customId) if customId.value != "true" => customId.value
        case _                                          => node.id
      }
      val customId = if (baseId.contains(dialect.location().getOrElse(""))) {
        baseId.replace(dialect.id, "")
      } else {
        baseId
      }
      Seq(MapEntryEmitter("$id", customId))
    } else Nil
  }

  private def customIdOf(node: DialectDomainElement) = node.annotations.find(classOf[CustomId])

  private def hasCustomId(node: DialectDomainElement) = customIdOf(node).isDefined

  private def emitDiscriminator(): Seq[EntryEmitter] = {
    discriminator.map { case (name, value) => Seq(MapEntryEmitter(name, value)) }.getOrElse(Seq.empty)
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
            emitters ++= nextEmitter
          }
        }
    }
    emitters
  }

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
    Seq(ObjectPairEmitter(key, array, propertyMapping, annotations))
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
