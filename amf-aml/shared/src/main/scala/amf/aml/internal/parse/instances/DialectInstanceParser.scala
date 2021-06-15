package amf.aml.internal.parse.instances

import amf.core.client.scala.model.DataType
import amf.core.internal.annotations.SourceAST
import amf.core.internal.metamodel.{Field, Type}
import amf.core.internal.metamodel.Type.{Array, Str}
import amf.core.client.scala.model.document.EncodesModel
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar, Annotation, DomainElement}
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.internal.parser.domain.{Annotations, SearchScope, _}
import amf.core.internal.utils._
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.parser.{Root, YMapOps, YNodeLikeOps}
import amf.aml.internal.annotations.{
  CustomBase,
  CustomId,
  DiscriminatorField,
  FromUnionNodeMapping,
  JsonPointerRef,
  RefInclude
}
import amf.aml.internal.metamodel.document.DialectInstanceModel
import amf.aml.internal.metamodel.domain.{DialectDomainElementModel, NodeWithDiscriminatorModel}
import amf.aml.client.scala.model.document._
import amf.aml.client.scala.model.domain._
import amf.aml.internal.parse.common.{AnnotationsParser, DeclarationKey, DeclarationKeyCollector}
import amf.aml.internal.parse.instances.ClosedInstanceNode.{checkClosedNode, checkRootNode}
import amf.aml.internal.validate.DialectValidations.{DialectAmbiguousRangeSpecification, DialectError, InvalidUnionType}
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model._

import scala.collection.mutable

// TODO: needs further breakup of parts. This components of this class are untestable the current way.
// TODO: find out why all these methods are protected.
// TODO:
class DialectInstanceParser(val root: Root)(implicit override val ctx: DialectInstanceContext)
    extends AnnotationsParser
    with DeclarationKeyCollector
    with JsonPointerResolver
    with InstanceNodeIdHandling {

  val map: YMap = root.parsed.asInstanceOf[SyamlParsedDocument].document.as[YMap]

  def parseDocument(): DialectInstance = {
    val dialectInstance: DialectInstance = DialectInstance(Annotations(map))
      .withLocation(root.location)
      .withId(root.location)
      .withDefinedBy(ctx.dialect.id)
    parseDeclarations("root")

    val references =
      DialectInstanceReferencesParser(dialectInstance, map, root.references)
        .parse(dialectInstance.location().getOrElse(dialectInstance.id))

    if (ctx.declarations.externals.nonEmpty)
      dialectInstance.withExternals(ctx.declarations.externals.values.toSeq)

    val dialectDomainElement = parseEncoded(dialectInstance)
    // registering JSON pointer
    ctx.registerJsonPointerDeclaration(root.location + "#/", dialectDomainElement)

    dialectInstance.set(DialectInstanceModel.Encodes, dialectDomainElement, Annotations.inferred())
    addDeclarationsToModel(dialectInstance)

    if (references.baseUnitReferences().nonEmpty)
      dialectInstance.withReferences(references.baseUnitReferences())
    if (ctx.nestedDialects.nonEmpty)
      dialectInstance.withGraphDependencies(ctx.nestedDialects.map(nd => nd.location().getOrElse(nd.id)))

    // resolve unresolved references
    ctx.futureDeclarations.resolve()

    dialectInstance
  }

  @scala.annotation.tailrec
  private def findDeclarationsMap(paths: List[String], map: YMap): Option[YMap] = {
    paths match {
      case Nil => Some(map)
      case head :: tail =>
        map.key(head) match {
          case Some(m) if m.value.tagType == YType.Map =>
            if (tail.nonEmpty) findDeclarationsMap(tail, m.value.as[YMap])
            else m.value.toOption[YMap]
          case Some(o) =>
            ctx.eh
              .violation(DialectError, "", s"Invalid node type for declarations path ${o.value.tagType.toString()}", o)
            None
          case _ => None
        }
    }
  }

  protected def parseDeclarations(documentType: String): Unit = {
    val declarationsNodeMappings = if (documentType == "root") {
      ctx.rootDeclarationsNodeMappings
    } else {
      ctx.libraryDeclarationsNodeMappings
    }

    val pathOption = Option(ctx.dialect.documents()).flatMap(d => d.declarationsPath().option())
    val normalizedPath =
      pathOption.map(p => if (p.startsWith("/")) p else "/" + p).map(p => if (p.endsWith("/")) p else p + "/")
    val paths: List[String] = pathOption.map(_.split("/").toList).getOrElse(Nil)
    findDeclarationsMap(paths, map).foreach { declarationsMap =>
      declarationsNodeMappings.foreach {
        case (name, nodeMapping) =>
          declarationsMap.entries.find(_.key.as[YScalar].text == name).foreach { entry =>
            addDeclarationKey(DeclarationKey(entry))
            val declarationsId = root.location + "#" + normalizedPath.getOrElse("/") + name.urlComponentEncoded
            entry.value.as[YMap].entries.foreach { declarationEntry =>
              val declarationName = declarationEntry.key.as[YScalar].text
              val id              = pathSegment(declarationsId, List(declarationName))
              val node = parseNode(declarationsId,
                                   id,
                                   declarationEntry.value,
                                   nodeMapping,
                                   givenAnnotations = Some(Annotations(declarationEntry)))

              // lookup by ref name
              node.set(DialectDomainElementModel.DeclarationName,
                       AmfScalar(declarationName, Annotations(declarationEntry.key)),
                       Annotations(declarationEntry.key))
              ctx.declarations.registerDialectDomainElement(declarationEntry.key, node)
              // lookup by JSON pointer, absolute URI
              ctx.registerJsonPointerDeclaration(id, node)
            }
          }
      }
    }
  }

  protected def encodedElementDefaultId(dialectInstance: EncodesModel): String =
    if (Option(ctx.dialect.documents()).flatMap(_.selfEncoded().option()).getOrElse(false))
      dialectInstance.location().getOrElse(dialectInstance.id)
    else
      dialectInstance.id + "#/encodes"

  protected def parseEncoded(dialectInstance: EncodesModel): DialectDomainElement = {
    val result = for {
      documents <- Option(ctx.dialect.documents())
      mapping   <- Option(documents.root())
    } yield {
      ctx.findNodeMapping(mapping.encoded().value()) match {
        case Some(nodeMapping) =>
          val path = dialectInstance.id + "#"
          val additionalKey =
            if (documents.keyProperty().value()) {
              Some(ctx.dialect.name().value())
            } else None
          parseNode(path,
                    encodedElementDefaultId(dialectInstance),
                    map,
                    nodeMapping,
                    rootNode = true,
                    givenAnnotations = None,
                    additionalKey = additionalKey)
        case _ =>
          emptyElementWithViolation(s"Could not find node mapping for: ${mapping.encoded().value()}")
      }
    }
    result.getOrElse {
      emptyElementWithViolation("Could not find root document mapping from dialect")
    }
  }

  protected def emptyElementWithViolation(msg: String)(implicit ctx: DialectInstanceContext): DialectDomainElement = {
    val empty = DialectDomainElement(map).withId(root.location)
    ctx.eh.violation(DialectError, empty.id, msg, map)
    empty
  }

  private def checkNodeForAdditionalKeys(id: String,
                                         nodetype: String,
                                         entries: Map[YNode, YNode],
                                         mapping: NodeMapping,
                                         ast: YPart,
                                         rootNode: Boolean,
                                         additionalKey: Option[String]): Unit = {
    if (rootNode) checkRootNode(id, nodetype, entries, mapping, ast, additionalKey)
    else checkClosedNode(id, nodetype, entries, mapping, ast, additionalKey)
  }

  protected def parseNode(path: String,
                          defaultId: String,
                          ast: YNode,
                          mappable: NodeMappable,
                          additionalProperties: Map[String, Any] = Map(),
                          rootNode: Boolean = false,
                          givenAnnotations: Option[Annotations],
                          additionalKey: Option[String] = None): DialectDomainElement = {
    val result: DialectDomainElement = ast.tagType match {
      case YType.Map =>
        val nodeMap = ast.as[YMap]
        dispatchNodeMap(nodeMap) match {
          case "$ref" =>
            val ref = resolveJSONPointer(nodeMap, mappable, defaultId)
            ref.annotations += JsonPointerRef()
            mappable match {
              case m: NodeMapping => ref.withDefinedBy(m)
              case _              => // ignore
            }
            ref
          case "$include" =>
            val link = resolveLink(ast, mappable, defaultId, givenAnnotations)
            link.annotations += RefInclude()
            link
          case _ =>
            mappable match {
              case mapping: NodeMapping =>
                val node: DialectDomainElement =
                  DialectDomainElement(givenAnnotations.getOrElse(Annotations(ast))).withDefinedBy(mapping)
                val finalId =
                  generateNodeId(node, nodeMap, Seq(defaultId), defaultId, mapping, additionalProperties, rootNode)
                node.withId(finalId)
                parseAnnotations(nodeMap, node, ctx.declarations)
                mapping.propertiesMapping().foreach { propertyMapping =>
                  val propertyName = propertyMapping.name().value()
                  nodeMap.key(propertyName) match {
                    case Some(entry) =>
                      val nestedId =
                        if (Option(ctx.dialect.documents())
                              .flatMap(_.selfEncoded().option())
                              .getOrElse(false) && rootNode)
                          defaultId + "#/"
                        else defaultId
                      parseProperty(nestedId, entry, propertyMapping, node)
                    case None => // ignore
                  }
                }
                checkNodeForAdditionalKeys(finalId, mapping.id, nodeMap.map, mapping, nodeMap, rootNode, additionalKey)
                node

              case unionMapping: UnionNodeMapping =>
                parseObjectUnion(defaultId, Seq(path), ast, unionMapping, additionalProperties)
            }

        }

      case YType.Str     => resolveLink(ast, mappable, defaultId, givenAnnotations)
      case YType.Include => resolveLink(ast, mappable, defaultId, givenAnnotations)
      case YType.Null =>
        emptyElement(defaultId, ast, mappable, givenAnnotations)
      case _ =>
        ctx.eh.violation(DialectError, defaultId, "Cannot parse AST node for node in dialect instance", ast)
        DialectDomainElement().withId(defaultId)
    }
    // if we are parsing a patch document we mark the node as abstract

    if (ctx.isPatch) result.withAbstract(true)
    mappable match {
      case mapping: NodeMapping =>
        result
          .withDefinedBy(mapping)
          .withInstanceTypes(typesFrom(mapping))
      case _ => // ignore
    }
    result
  }

  private def typesFrom(mapping: NodeMapping): Seq[String] = {
    Seq(mapping.nodetypeMapping.option(), Some(mapping.id)).flatten
  }

  private def emptyElement(defaultId: String,
                           ast: YNode,
                           mappable: NodeMappable,
                           givenAnnotations: Option[Annotations]): DialectDomainElement = {
    val mappings = mappable match {
      case m: NodeMapping => Seq(m.nodetypeMapping.value(), mappable.id)
      case _              => Seq(mappable.id)
    }
    lazy val ann = mappable match {
      case u: UnionNodeMapping => Annotations(ast) += FromUnionNodeMapping(u)
      case _                   => Annotations(ast)
    }
    val element = DialectDomainElement(givenAnnotations.getOrElse(ann))
      .withId(defaultId)
      .withInstanceTypes(mappings)
    ctx.eh.warning(DialectError, defaultId, s"Empty map: ${mappings.head}", ast)
    element
  }

  protected def parseProperty(id: String,
                              propertyEntry: YMapEntry,
                              property: PropertyMapping,
                              node: DialectDomainElement): Unit = {
    property.classification() match {
      case ExtensionPointProperty    => parseDialectExtension(id, propertyEntry, property, node)
      case LiteralProperty           => parseLiteralProperty(id, propertyEntry, property, node)
      case LiteralPropertyCollection => parseLiteralCollectionProperty(id, propertyEntry, property, node)
      case ObjectProperty            => parseObjectProperty(id, propertyEntry, property, node)
      case ObjectPropertyCollection  => parseObjectCollectionProperty(id, propertyEntry, property, node)
      case ObjectMapProperty         => parseObjectMapProperty(id, propertyEntry, property, node)
      case ObjectPairProperty        => parseObjectPairProperty(id, propertyEntry, property, node)
      case ExternalLinkProperty      => parseExternalLinkProperty(id, propertyEntry, property, node)
      case _ =>
        ctx.eh.violation(DialectError, id, s"Unknown type of node property ${property.id}", propertyEntry)
    }
  }

  private def generateExternalLink(id: String, node: YNode, mapping: NodeMapping): Option[DialectDomainElement] = {
    lazy val instanceTypes = typesFrom(mapping)
    node.tagType match {
      case YType.Str => // plain link -> we generate an anonymous node and set the id to the ref and correct type information
        val elem = DialectDomainElement()
          .withDefinedBy(mapping)
          .withId(node)
          .withIsExternalLink(true)
          .withInstanceTypes(instanceTypes)
        Some(elem)

      case YType.Map
          if node
            .as[YMap]
            .key("$id")
            .isDefined => // simple link in a reference map
        val refMap = node.as[YMap]

        val id      = explicitNodeId(None, refMap, Nil, "", mapping)
        val finalId = overrideBase(id, refMap)

        val elem = DialectDomainElement().withDefinedBy(mapping).withId(finalId).withIsExternalLink(true)
        elem.withInstanceTypes(instanceTypes)
        elem.annotations += CustomId()
        refMap.key("$base") match {
          case Some(baseEntry) =>
            elem.annotations += CustomBase(baseEntry.value.toString)
          case _ => // Nothing
        }
        Some(elem)

      case YType.Map if mapping.idTemplate.nonEmpty => // complex reference with mandatory idTemplate
        val refMap = node.as[YMap]

        val element = DialectDomainElement(Annotations(refMap))
        val id      = idTemplate(element, refMap, Nil, mapping)
        val finalId = overrideBase(id, refMap)

        // Now we actually parse the provided properties for the node
        val linkReference: DialectDomainElement =
          element
            .withDefinedBy(mapping)
            .withId(finalId)
            .withInstanceTypes(instanceTypes)
            .withIsExternalLink(true) // this is a linkReference

        refMap.key("$base") match {
          case Some(baseEntry) =>
            linkReference.annotations += CustomBase(baseEntry.value.toString)
          case _ => // Nothing
        }

        // TODO why do we parse properties?
        mapping.propertiesMapping().foreach { propertyMapping =>
          val propertyName = propertyMapping.name().value()
          refMap.key(propertyName) match {
            case Some(entry) =>
              parseProperty(finalId, entry, propertyMapping, linkReference)
            case None => // ignore
          }
        }

        // return the parsed reference
        Some(linkReference)

      case _ => // error
        ctx.eh.violation(
            DialectError,
            id,
            "AML links must URI links (strings or maps with $id directive) or ID Template links (maps with idTemplate variables)")
        None
    }
  }

  private def parseExternalLinkProperty(id: String,
                                        propertyEntry: YMapEntry,
                                        property: PropertyMapping,
                                        node: DialectDomainElement): Unit = {
    // First extract the mapping information
    // External links only work over single ranges, not unions or literal ranges
    val maybeMapping: Option[NodeMapping] = property.objectRange() match {
      case range if range.length == 1 =>
        ctx.dialect.declares.find(_.id == range.head.value()) match {
          case Some(nodeMapping: NodeMapping) =>
            Some(nodeMapping)
          case _ =>
            ctx.eh.violation(DialectError, id, s"Cannot find object range ${range.head.value()}", propertyEntry)
            None
        }
      case _ =>
        ctx.eh
          .violation(DialectError, id, s"Individual object range required for external link property", propertyEntry)
        None
    }

    val allowMultiple = property.allowMultiple().option().getOrElse(false)

    // now we parse the link
    maybeMapping match {
      case Some(mapping) =>
        val rangeNode = propertyEntry.value
        rangeNode.tagType match {
          case YType.Str if !allowMultiple =>
            // plain link -> we generate an anonymous node and set the id to the ref and correct type information
            generateExternalLink(id, rangeNode, mapping).foreach { elem =>
              node.setObjectField(property, elem, Right(propertyEntry))
            }
          case YType.Map if !allowMultiple => // reference
            val refMap = rangeNode.as[YMap]
            generateExternalLink(id, refMap, mapping) foreach { elem =>
              node.setObjectField(property, elem, Right(propertyEntry))
            }
          case YType.Seq if allowMultiple => // sequence of links or references
            val seq = rangeNode.as[YSequence]
            val elems = seq.nodes.flatMap { node =>
              generateExternalLink(id, node, mapping)
            }
            if (elems.nonEmpty)
              node.setObjectField(property, elems, Right(propertyEntry))
          case YType.Seq if !allowMultiple => // error
            ctx.eh.violation(DialectError,
                             id,
                             s"AllowMultiple not enable, sequence of external links not supported",
                             propertyEntry)
          case _ =>
            ctx.eh.violation(DialectError, id, s"Not supported external link range", propertyEntry)
        }
      case _ => // ignore
    }
  }

  protected def parseDialectExtension(id: String,
                                      propertyEntry: YMapEntry,
                                      property: PropertyMapping,
                                      node: DialectDomainElement): Unit = {
    val nestedObjectId = pathSegment(id, List(propertyEntry.key.as[YScalar].text))
    propertyEntry.value.tagType match {
      case YType.Str | YType.Include =>
        resolveLinkProperty(propertyEntry, property, nestedObjectId, node)
      case YType.Map =>
        val map = propertyEntry.value.as[YMap]
        map.key("$dialect") match {
          case Some(nested) if nested.value.tagType == YType.Str =>
            val dialectNode = nested.value.as[YScalar].text
            // TODO: resolve dialect node URI to absolute normalised URI
            ctx.nodeMappableFinder.findNode(dialectNode) match {
              case Some((dialect, nodeMapping)) =>
                ctx.nestedDialects ++= Seq(dialect)
                ctx.withCurrentDialect(dialect) {
                  val dialectDomainElement = parseNestedNode(id, nestedObjectId, propertyEntry.value, nodeMapping)
                  node.setObjectField(property, dialectDomainElement, Right(propertyEntry))
                }
              case None =>
                ctx.eh.violation(DialectError,
                                 id,
                                 s"Cannot find dialect for nested anyNode mapping $dialectNode",
                                 nested.value)
            }
          case None =>
            dispatchNodeMap(map) match {
              case "$include" =>
                val includeEntry = map.key("$include").get
                resolveLinkProperty(includeEntry, property, nestedObjectId, node, isRef = true)
              case "$ref" =>
                resolveJSONPointerProperty(map, property, nestedObjectId, node)
              case _ =>
                ctx.eh.violation(DialectError, id, "$dialect key without string value or link", map)
            }
        }
    }
  }

  protected def findHashProperties(propertyMapping: PropertyMapping, propertyEntry: YMapEntry): Option[(String, Any)] = {
    propertyMapping.mapTermKeyProperty().option() match {
      case Some(propId) => Some((propId, propertyEntry.key.as[YScalar].text))
      case None         => None
    }
  }

  protected def checkHashProperties(node: DialectDomainElement,
                                    propertyMapping: PropertyMapping,
                                    propertyEntry: YMapEntry): DialectDomainElement = {
    // TODO: check if the node already has a value and that it matches (maybe coming from a declaration)
    propertyMapping.mapTermKeyProperty().option() match {
      case Some(propId) =>
        try {
          node.set(Field(Str, ValueType(propId)),
                   AmfScalar(propertyEntry.key.as[YScalar].text),
                   Annotations(propertyEntry.key))
        } catch {
          case e: UnknownMapKeyProperty =>
            ctx.eh.violation(DialectError, e.id, s"Cannot find mapping for key map property ${e.id}")
            node
        }
      case None => node
    }
  }

  protected def findCompatibleMapping(id: String,
                                      unionMappings: Seq[NodeMapping],
                                      discriminatorMapping: Map[String, NodeMapping],
                                      discriminator: Option[String],
                                      nodeMap: YMap,
                                      additionalProperties: Seq[String]): Seq[NodeMapping] = {
    discriminator match {
      // Using explicit discriminator
      case Some(propertyName) =>
        val explicitMapping = nodeMap.entries.find(_.key.as[YScalar].text == propertyName).flatMap { entry =>
          discriminatorMapping.get(entry.value.as[YScalar].text)
        }
        explicitMapping match {
          case Some(nodeMapping) => Seq(nodeMapping)
          case None =>
            ctx.eh.violation(DialectError,
                             id,
                             s"Cannot find discriminator value for discriminator '$propertyName'",
                             nodeMap)
            Nil
        }
      // Inferring based on properties
      case None =>
        val properties: Set[String] = nodeMap.entries.map(_.key.as[YScalar].text).toSet
        unionMappings.filter { mapping =>
          val baseProperties =
            mapping.propertiesMapping().filter(pm => !additionalProperties.contains(pm.nodePropertyMapping().value()))
          val mappingRequiredSet: Set[String] = baseProperties
            .filter(_.minCount().value() > 0)
            .map(_.name().value())
            .toSet
          val mappingSet: Set[String] = baseProperties
            .map(_.name().value())
            .toSet

          // There are not additional properties in the set and all required properties are in the set
          properties.diff(mappingSet).isEmpty && mappingRequiredSet.diff(properties).isEmpty
        }
    }
  }

  private def discriminatorAnnotation(discriminatorName: Option[String], nodeMap: YMap): Option[Annotation] = {
    discriminatorName.flatMap { propertyName =>
      nodeMap.entries.find(_.key.as[YScalar].text == propertyName).map { entry =>
        DiscriminatorField(propertyName, entry.value.as[YScalar].text)
      }
    }
  }

  protected def parseObjectUnion[T <: DomainElement](
      defaultId: String,
      path: Seq[String],
      ast: YNode,
      unionMapping: NodeWithDiscriminator[_ <: NodeWithDiscriminatorModel],
      additionalProperties: Map[String, Any] = Map()): DialectDomainElement = {

    // potential node range based in the objectRange
    val unionMembers: Seq[NodeMapping] = unionMapping.objectRange().flatMap { memberId =>
      ctx.dialect.declares.find(_.id == memberId.value()) match {
        case Some(nodeMapping: NodeMapping) => Some(nodeMapping)
        case _ =>
          ctx.eh
            .violation(DialectError, defaultId, s"Cannot find mapping for property ${unionMapping.id} in union", ast)
          None
      }
    }

    // potential node range based in discriminators map
    val discriminatorsMapping: Map[String, NodeMapping] = {
      Option(unionMapping.typeDiscriminator()) match {
        case Some(discriminatorValueMapping) =>
          discriminatorValueMapping.flatMap {
            case (discriminatorValue, nodeMappingId) =>
              ctx.dialect.declares.find(_.id == nodeMappingId) match {
                case Some(nodeMapping: NodeMapping) => Some(discriminatorValue -> nodeMapping)
                case _ =>
                  ctx.eh.violation(
                      DialectError,
                      defaultId,
                      s"Cannot find mapping for property $nodeMappingId in discriminator value '$discriminatorValue' in union",
                      ast)
                  None
              }
          }
        case None =>
          Map.empty
      }
    }

    // all possible mappings combining objectRange and type discriminator
    // TODO: we should choose either of these, not both
    // TODO: if these sets are non-equal we should throw a validation at dialect definition time
    val allPossibleMappings = (unionMembers ++ discriminatorsMapping.values).distinct

    ast.tagType match {
      case YType.Map =>
        val nodeMap = ast.as[YMap]
        val annotations: Annotations = unionMapping match {
          case unionNodeMapping: UnionNodeMapping => Annotations(nodeMap) += FromUnionNodeMapping(unionNodeMapping)
          case _                                  => Annotations(nodeMap)
        }
        dispatchNodeMap(nodeMap) match {
          case "$include" =>
            val link = resolveLinkUnion(ast, allPossibleMappings, defaultId)
            link.annotations += RefInclude()
            link
          case "$ref" =>
            val ref = resolveJSONPointerUnion(nodeMap, allPossibleMappings, defaultId)
            ref.annotations += JsonPointerRef()
            ref
          case _ =>
            val discriminatorName = unionMapping.typeDiscriminatorName().option()
            val mappings = findCompatibleMapping(defaultId,
                                                 unionMembers,
                                                 discriminatorsMapping,
                                                 discriminatorName,
                                                 nodeMap,
                                                 additionalProperties.keys.toSeq)
            if (mappings.isEmpty) {
              ctx.eh.violation(
                  DialectAmbiguousRangeSpecification,
                  defaultId,
                  s"Ambiguous node in union range, found 0 compatible mappings from ${allPossibleMappings.size} mappings: [${allPossibleMappings.map(_.id).mkString(",")}]",
                  ast
              )
              DialectDomainElement(annotations)
                .withId(defaultId)
            } else if (mappings.size == 1) {
              val node: DialectDomainElement = DialectDomainElement(annotations).withDefinedBy(mappings.head)
              val finalId =
                generateNodeId(node, nodeMap, path, defaultId, mappings.head, additionalProperties, rootNode = false)
              node.withId(finalId)
              var instanceTypes: Seq[String] = Nil
              mappings.foreach { mapping =>
                val beforeValues = node.fields.fields().size
                mapping.propertiesMapping().foreach { propertyMapping =>
                  if (!node.containsProperty(propertyMapping)) {
                    val propertyName = propertyMapping.name().value()

                    nodeMap.entries.find(_.key.as[YScalar].text == propertyName) match {
                      case Some(entry) => parseProperty(finalId, entry, propertyMapping, node)
                      case None        => // ignore
                    }
                  }
                }
                val afterValues = node.fields.fields().size
                if (afterValues != beforeValues && mapping.nodetypeMapping.nonEmpty) {
                  instanceTypes ++= Seq(mapping.nodetypeMapping.value())
                }
              }
              node.withInstanceTypes(instanceTypes ++ Seq(mappings.head.id))
              discriminatorAnnotation(discriminatorName, nodeMap).foreach(node.add)
              checkNodeForAdditionalKeys(finalId,
                                         mappings.head.id,
                                         nodeMap.map,
                                         mappings.head,
                                         nodeMap,
                                         rootNode = false,
                                         discriminatorName)
              node
            } else {
              ctx.eh.violation(
                  DialectAmbiguousRangeSpecification,
                  defaultId,
                  None, // Some(property.nodePropertyMapping().value()),
                  s"Ambiguous node, please provide a type disambiguator. Nodes ${mappings.map(_.id).mkString(",")} have been found compatible, only one is allowed",
                  map
              )
              DialectDomainElement(annotations)
                .withId(defaultId)
            }
        }

      case YType.Str | YType.Include => // here the mapping information is explicit in the fragment/declaration mapping
        resolveLinkUnion(ast, allPossibleMappings, defaultId)

      case _ =>
        ctx.eh.violation(InvalidUnionType, defaultId, "Cannot parse AST for union node mapping", ast)
        DialectDomainElement().withId(defaultId)
    }
  }

  protected def parseObjectProperty(id: String,
                                    propertyEntry: YMapEntry,
                                    property: PropertyMapping,
                                    node: DialectDomainElement,
                                    additionalProperties: Map[String, Any] = Map()): Unit = {
    val path           = propertyEntry.key.as[YScalar].text
    val nestedObjectId = pathSegment(id, List(path))
    property.nodesInRange match {
      case range: Seq[String] if range.size > 1 =>
        val parsedRange =
          parseObjectUnion(nestedObjectId, Seq(path), propertyEntry.value, property, additionalProperties)
        node.setObjectField(property, parsedRange, Right(propertyEntry))
      case range: Seq[String] if range.size == 1 =>
        ctx.dialect.declares.find(_.id == range.head) match {
          case Some(nodeMapping: NodeMappable) =>
            val dialectDomainElement =
              parseNestedNode(id, nestedObjectId, propertyEntry.value, nodeMapping, additionalProperties)
            node.setObjectField(property, dialectDomainElement, Right(propertyEntry))
          case _ => // ignore
        }
      case _ => // TODO: throw exception, illegal range
    }
  }

  protected def parseObjectMapProperty(id: String,
                                       propertyEntry: YMapEntry,
                                       property: PropertyMapping,
                                       node: DialectDomainElement,
                                       additionalProperties: Map[String, Any] = Map()): Unit = {
    val nested = propertyEntry.value.as[YMap].entries.map { keyEntry =>
      val path           = List(propertyEntry.key.as[YScalar].text, keyEntry.key.as[YScalar].text)
      val nestedObjectId = pathSegment(id, path)
      // we add the potential mapKey additional property
      val keyAdditionalProperties: Map[String, Any] = findHashProperties(property, keyEntry) match {
        case Some((k, v)) => additionalProperties + (k -> v)
        case _            => additionalProperties
      }
      val parsedNode = property.nodesInRange match {
        case range: Seq[String] if range.size > 1 =>
          // we also add the potential mapValue property
          val keyValueAdditionalProperties = property.mapTermValueProperty().option() match {
            case Some(mapValueProperty) => keyAdditionalProperties + (mapValueProperty -> "")
            case _                      => keyAdditionalProperties
          }
          // now we can parse the union with all the required properties to find the right node in the union
          Some(parseObjectUnion(nestedObjectId, path, keyEntry.value, property, keyValueAdditionalProperties))
        case range: Seq[String] if range.size == 1 =>
          ctx.dialect.declares.find(_.id == range.head) match {
            case Some(nodeMapping: NodeMappable) if keyEntry.value.tagType != YType.Null =>
              Some(parseNestedNode(id, nestedObjectId, keyEntry.value, nodeMapping, keyAdditionalProperties))
            case _ => None
          }
        case _ => None
      }
      parsedNode match {
        case Some(dialectDomainElement) => Some(checkHashProperties(dialectDomainElement, property, keyEntry))
        case None                       => None
      }
    }
    node.setObjectField(property, nested.flatten, Right(propertyEntry))
  }

  protected def parseObjectPairProperty(id: String,
                                        propertyEntry: YMapEntry,
                                        property: PropertyMapping,
                                        node: DialectDomainElement): Unit = {
    val propertyKeyMapping   = property.mapTermKeyProperty().option()
    val propertyValueMapping = property.mapTermValueProperty().option()
    if (propertyKeyMapping.isDefined && propertyValueMapping.isDefined) {
      val nested = ctx.dialect.declares.find(_.id == property.objectRange().head.value()) match {
        case Some(nodeMapping: NodeMapping) =>
          propertyEntry.value.as[YMap].entries flatMap { pair: YMapEntry =>
            val nestedId = id + "/" + propertyEntry.key.as[YScalar].text.urlComponentEncoded + "/" + pair.key
              .as[YScalar]
              .text
              .urlComponentEncoded
            val effectiveTypes      = typesFrom(nodeMapping)
            val valueAllowsMultiple = extractAllowMultipleForProp(propertyValueMapping, nodeMapping).getOrElse(false)
            val nestedNode = DialectDomainElement(Annotations(pair))
              .withId(nestedId)
              .withDefinedBy(nodeMapping)
              .withInstanceTypes(effectiveTypes)
            try {
              nestedNode.set(Field(Str, ValueType(propertyKeyMapping.get)),
                             AmfScalar(pair.key.as[YScalar].text),
                             Annotations(pair.key))

              if (valueAllowsMultiple) {
                pair.value.value match {
                  case seq: YSequence =>
                    nestedNode.set(
                        Field(Array(Str), ValueType(propertyValueMapping.get)),
                        AmfArray(seq.nodes.flatMap(_.asScalar).map(AmfScalar(_)), Annotations(seq)),
                        Annotations(pair.value)
                    )
                  case scalar: YScalar =>
                    nestedNode.set(Field(Array(Str), ValueType(propertyValueMapping.get)),
                                   AmfArray(Seq(AmfScalar(scalar.text))),
                                   Annotations(pair.value))
                  case _ => // ignore
                }
              } else {
                nestedNode.set(Field(Str, ValueType(propertyValueMapping.get)),
                               AmfScalar(pair.value.as[YScalar].text),
                               Annotations(pair.value))
              }
            } catch {
              case e: UnknownMapKeyProperty =>
                ctx.eh.violation(DialectError, e.id, s"Cannot find mapping for key map property ${e.id}", pair)
            }
            Some(nestedNode)
          }
        case _ =>
          ctx.eh.violation(
              DialectError,
              id,
              s"Cannot find mapping for property range of mapValue property: ${property.objectRange().head.value()}",
              propertyEntry
          )
          Nil
      }

      node.setObjectField(property, nested, Left(propertyEntry.key))

    } else {
      ctx.eh.violation(DialectError,
                       id,
                       s"Both 'mapKey' and 'mapValue' are mandatory in a map pair property mapping",
                       propertyEntry)
    }
  }

  private def extractAllowMultipleForProp(propertyValueMapping: Option[String], nodeMapping: NodeMapping) = {
    nodeMapping
      .propertiesMapping()
      .find(_.nodePropertyMapping().option().contains(propertyValueMapping.get))
      .flatMap(_.allowMultiple().option())
  }

  protected def parseObjectCollectionProperty(id: String,
                                              propertyEntry: YMapEntry,
                                              property: PropertyMapping,
                                              node: DialectDomainElement,
                                              additionalProperties: Map[String, Any] = Map()): Unit = {

    // just to store Ids, and detect potentially duplicated elements in the collection
    val idsMap: mutable.Map[String, Boolean] = mutable.Map()
    val entries = propertyEntry.value.tagType match {
      case YType.Seq => propertyEntry.value.as[YSequence].nodes
      case _         => Seq(propertyEntry.value)
    }

    val elems = entries.zipWithIndex.flatMap {
      case (elementNode, nextElem) =>
        val path           = List(propertyEntry.key.as[YScalar].text, nextElem.toString)
        val nestedObjectId = pathSegment(id, path)
        property.nodesInRange match {
          case range: Seq[String] if range.size > 1 =>
            val parsed = parseObjectUnion(nestedObjectId, path, elementNode, property, additionalProperties)
            checkDuplicated(parsed, elementNode, idsMap)
            Some(parsed)
          case range: Seq[String] if range.size == 1 =>
            ctx.dialect.declares.find(_.id == range.head) match {
              case Some(nodeMapping: NodeMappable) =>
                val dialectDomainElement =
                  parseNestedNode(id, nestedObjectId, elementNode, nodeMapping, additionalProperties)
                checkDuplicated(dialectDomainElement, elementNode, idsMap)
                Some(dialectDomainElement)
              case _ => None
            }
          case _ => None
        }
    }
    node.setObjectField(property, elems, Right(propertyEntry))
  }

  def checkDuplicated(dialectDomainElement: DialectDomainElement,
                      elementNode: YNode,
                      idsMap: mutable.Map[String, Boolean]): Unit = {
    idsMap.get(dialectDomainElement.id) match {
      case None => idsMap.update(dialectDomainElement.id, true)
      case _ =>
        ctx.eh.violation(DialectError,
                         dialectDomainElement.id,
                         s"Duplicated element in collection ${dialectDomainElement.id}",
                         elementNode)
    }
  }

  protected def pathSegment(parent: String, nexts: List[String]): String = {
    var path = parent
    nexts.foreach { n =>
      path = if (path.endsWith("/")) {
        path + n.urlComponentEncoded
      } else {
        path + "/" + n.urlComponentEncoded
      }
    }
    path
  }

  protected def parseLiteralValue(value: YNode, property: PropertyMapping, node: DialectDomainElement): Option[_] = {

    value.tagType match {
      case YType.Bool
          if (property.literalRange().value() == DataType.Boolean) || property
            .literalRange()
            .value() == DataType.Any =>
        Some(value.as[Boolean])
      case YType.Bool =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    DataType.Boolean,
                                                    value)
        None
      case YType.Int
          if property.literalRange().value() == DataType.Integer || property
            .literalRange()
            .value() == DataType.Number || property.literalRange().value() == DataType.Any =>
        Some(value.as[Int])
      case YType.Int
          if property.literalRange().value() == DataType.Float ||
            property.literalRange().value() == DataType.Double =>
        Some(value.as[Double])
      case YType.Int =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    DataType.Integer,
                                                    value)
        None
      case YType.Str
          if property.literalRange().value() == DataType.String || property.literalRange().value() == DataType.Any =>
        Some(value.as[YScalar].text)
      case YType.Str if property.literalRange().value() == DataType.AnyUri =>
        Some(value.as[YScalar].text)
      case YType.Str if property.literalRange().value() == (Namespace.Shapes + "link").iri() =>
        Some(("link", value.as[YScalar].text))
      case YType.Str
          if property.literalRange().value() == DataType.Time ||
            property.literalRange().value() == DataType.Date ||
            property.literalRange().value() == DataType.DateTime =>
        Some(YNode(value.value, YType.Timestamp).as[SimpleDateTime])
      case YType.Str if property.literalRange().value() == (Namespace.Shapes + "guid").iri() =>
        Some(value.as[YScalar].text)
      case YType.Str =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    DataType.String,
                                                    value)
        None
      case YType.Float
          if property.literalRange().value() == DataType.Float ||
            property.literalRange().value() == DataType.Number ||
            property.literalRange().value() == DataType.Double ||
            property.literalRange().value() == DataType.Any =>
        Some(value.as[Double])
      case YType.Float =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    DataType.Float,
                                                    value)
        None

      case YType.Timestamp
          if property.literalRange().value() == DataType.Time ||
            property.literalRange().value() == DataType.Date ||
            property.literalRange().value() == DataType.DateTime ||
            property.literalRange().value() == DataType.Any =>
        Some(value.as[SimpleDateTime])

      case YType.Timestamp if property.literalRange().value() == DataType.String =>
        Some(value.as[YScalar].text)

      case YType.Timestamp =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    DataType.DateTime,
                                                    value)
        Some(value.as[String])

      case YType.Null =>
        None
      case _ =>
        ctx.eh.violation(DialectError, node.id, s"Unsupported scalar type ${value.tagType}", value)
        Some(value.as[String])
    }
  }

  protected def setLiteralValue(entry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    parseLiteralValue(entry.value, property, node) match {
      case Some(b: Boolean)          => node.setProperty(property, b, entry)
      case Some(i: Int)              => node.setProperty(property, i, entry)
      case Some(f: Float)            => node.setProperty(property, f, entry)
      case Some(d: Double)           => node.setProperty(property, d, entry)
      case Some(s: String)           => node.setProperty(property, s, entry)
      case Some(("link", l: String)) => node.setProperty(property, l, entry)
      case Some(d: SimpleDateTime)   => node.setProperty(property, d, entry)
      case _                         => node.setProperty(property, entry)
    }
  }

  protected def parseLiteralProperty(id: String,
                                     propertyEntry: YMapEntry,
                                     property: PropertyMapping,
                                     node: DialectDomainElement): Unit = {
    setLiteralValue(propertyEntry, property, node)
  }

  protected def parseLiteralCollectionProperty(id: String,
                                               propertyEntry: YMapEntry,
                                               property: PropertyMapping,
                                               node: DialectDomainElement): Unit = {
    val finalValues = propertyEntry.value.tagType match {
      case YType.Seq =>
        val values = propertyEntry.value
          .as[YSequence]
          .nodes
          .flatMap { elemValue =>
            parseLiteralValue(elemValue, property, node)
          }

        values.headOption match {
          case Some(("link", _: String)) => values.collect { case (_, link) => link }.asInstanceOf[Seq[String]]
          case _                         => values
        }

      case _ =>
        parseLiteralValue(propertyEntry.value, property, node) match {
          case Some(("link", v)) => Seq(v)
          case Some(v)           => Seq(v)
          case _                 => Nil
        }

    }
    node.setProperty(property, finalValues, propertyEntry)

  }

  protected def parseNestedNode(path: String,
                                id: String,
                                entry: YNode,
                                mapping: NodeMappable,
                                additionalProperties: Map[String, Any] = Map()): DialectDomainElement =
    parseNode(path, id, entry, mapping, additionalProperties, givenAnnotations = None)

  protected def dispatchNodeMap(nodeMap: YMap): String =
    if (nodeMap.key("$include").isDefined)
      "$include"
    else if (nodeMap.key("$ref").isDefined)
      "$ref"
    else
      "inline"

  protected def resolveLink(ast: YNode,
                            mapping: NodeMappable,
                            id: String,
                            givenAnnotations: Option[Annotations]): DialectDomainElement = {
    val refTuple = ctx.link(ast) match {
      case Left(key) =>
        (key, ctx.declarations.findDialectDomainElement(key, mapping, SearchScope.Fragments))
      case _ =>
        val text = ast.as[YScalar].text
        (text, ctx.declarations.findDialectDomainElement(text, mapping, SearchScope.Named))
    }
    refTuple match {
      case (text: String, Some(s)) =>
        val linkedNode = s
          .link(text, givenAnnotations.getOrElse(Annotations(ast)))
          .asInstanceOf[DialectDomainElement]
          .withInstanceTypes(Seq(mapping.id))
          .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
        linkedNode
      case (text: String, _) =>
        val linkedNode = DialectDomainElement(givenAnnotations.getOrElse(Annotations(ast)))
          .withId(id)
          .withInstanceTypes(Seq(mapping.id))
        linkedNode.unresolved(text, givenAnnotations.flatMap(_.find(classOf[SourceAST])).map(_.ast).getOrElse(ast))
        linkedNode
    }
  }

  protected def resolveLinkUnion(ast: YNode, allPossibleMappings: Seq[NodeMapping], id: String): DialectDomainElement = {
    val refTuple = ctx.link(ast) match {
      case Left(key) =>
        (key,
         allPossibleMappings
           .map(mapping => ctx.declarations.findDialectDomainElement(key, mapping, SearchScope.Fragments))
           .collectFirst { case Some(x) => x })
      case _ =>
        val text = ast.as[YScalar].text
        (text,
         allPossibleMappings
           .map(mapping => ctx.declarations.findDialectDomainElement(text, mapping, SearchScope.Named))
           .collectFirst { case Some(x) => x })
    }
    refTuple match {
      case (text: String, Some(s)) =>
        val linkedNode = s
          .link(text, Annotations(ast.value))
          .asInstanceOf[DialectDomainElement]
          .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
        linkedNode
      case (text: String, _) =>
        val linkedNode = DialectDomainElement(map).withId(id)
        linkedNode.unresolved(text, map)
        linkedNode
    }
  }

  protected def resolveJSONPointerUnion(map: YMap,
                                        allPossibleMappings: Seq[NodeMapping],
                                        id: String): DialectDomainElement = {
    val entry   = map.key("$ref").get
    val pointer = entry.value.as[String]
    val fullPointer = if (pointer.startsWith("#")) {
      root.location + pointer
    } else {
      pointer
    }
    ctx.findJsonPointer(fullPointer) map { node =>
      if (allPossibleMappings.exists(_.id == node.definedBy.id)) {
        node
          .link(pointer, Annotations(map))
          .asInstanceOf[DialectDomainElement]
          .withId(id)
      } else {
        val linkedNode = DialectDomainElement(map).withId(id)
        linkedNode.unresolved(fullPointer, map)
        linkedNode
      }
    } getOrElse {
      val linkedNode = DialectDomainElement(map).withId(id)
      linkedNode.unresolved(fullPointer, map)
      linkedNode
    }
  }

  protected def resolveLinkProperty(propertyEntry: YMapEntry,
                                    mapping: PropertyMapping,
                                    id: String,
                                    node: DialectDomainElement,
                                    isRef: Boolean = false): Unit = {
    val refTuple = ctx.link(propertyEntry.value) match {
      case Left(key) =>
        (key, ctx.declarations.findAnyDialectDomainElement(key, SearchScope.Fragments))
      case _ =>
        val text = propertyEntry.value.as[YScalar].text
        (text, ctx.declarations.findAnyDialectDomainElement(text, SearchScope.All))
    }
    refTuple match {
      case (text: String, Some(s)) =>
        ctx.nodeMappableFinder.findNode(s.definedBy.id) match {
          case Some((dialect, _)) =>
            ctx.nestedDialects ++= Seq(dialect)
            val linkedExternal = s
              .link(text, Annotations(propertyEntry.value))
              .asInstanceOf[DialectDomainElement]
              .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            if (isRef) linkedExternal.annotations += RefInclude()
            node.setObjectField(mapping, linkedExternal, Right(propertyEntry))
          case None =>
            ctx.eh.violation(DialectError,
                             id,
                             s"Cannot find dialect for anyNode node mapping ${s.definedBy.id}",
                             propertyEntry.value)
        }
      case _ =>
        ctx.eh.violation(
            DialectError,
            id,
            s"anyNode reference must be to a known node or an external fragment, unknown value: '${propertyEntry.value}'",
            propertyEntry.value
        )
    }
  }

  protected def resolveJSONPointerProperty(map: YMap,
                                           mapping: PropertyMapping,
                                           id: String,
                                           node: DialectDomainElement): Unit = {
    val entry   = map.key("$ref").get
    val pointer = entry.value.as[String]
    val fullPointer = if (pointer.startsWith("#")) {
      root.location + pointer
    } else {
      pointer
    }

    ctx.findJsonPointer(fullPointer) map { node =>
      node
        .link(pointer, Annotations(map))
        .asInstanceOf[DialectDomainElement]
        .withId(id)
    } match {
      case Some(s) =>
        ctx.nodeMappableFinder.findNode(s.definedBy.id) match {
          case Some((dialect, _)) =>
            ctx.nestedDialects ++= Seq(dialect)
            val linkedExternal = s
              .link(pointer, Annotations(map))
              .asInstanceOf[DialectDomainElement]
              .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            node.setObjectField(mapping, linkedExternal, Right(entry))
          case None =>
            ctx.eh.violation(DialectError, id, s"Cannot find dialect for anyNode node mapping ${s.definedBy.id}", map)
        }
      case None =>
        ctx.eh.violation(
            DialectError,
            id,
            s"anyNode reference must be to a known node or an external fragment, unknown JSON Pointer: '$pointer'",
            map
        )
    }
  }
}
