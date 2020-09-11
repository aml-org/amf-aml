package amf.plugins.document.vocabularies.parser.instances

import amf.core.Root
import amf.core.annotations.SourceAST
import amf.core.metamodel.Field
import amf.core.metamodel.Type.Str
import amf.core.model.DataType
import amf.core.model.document.EncodesModel
import amf.core.model.domain.{AmfScalar, Annotation, DomainElement}
import amf.core.parser.{Annotations, SearchScope, _}
import amf.core.utils._
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.annotations.{CustomId, DiscriminatorField, JsonPointerRef, RefInclude}
import amf.plugins.document.vocabularies.metamodel.domain.DialectDomainElementModel
import amf.plugins.document.vocabularies.model.document._
import amf.plugins.document.vocabularies.model.domain._
import amf.plugins.document.vocabularies.parser.common.AnnotationsParser
import amf.validation.DialectValidations.{DialectAmbiguousRangeSpecification, DialectError, InvalidUnionType}
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model._

import scala.collection.mutable

// TODO: needs further breakup of parts. This components of this class are untestable the current way.
// TODO: find out why all these methods are protected.
// TODO:
class DialectInstanceParser(val root: Root)(implicit override val ctx: DialectInstanceContext)
    extends AnnotationsParser
    with JsonPointerResolver {

  val map: YMap = root.parsed.asInstanceOf[SyamlParsedDocument].document.as[YMap]

  def parseDocument(): Option[DialectInstance] = {
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

    val document = parseEncoded(dialectInstance) match {

      case Some(dialectDomainElement) =>
        // registering JSON pointer
        ctx.registerJsonPointerDeclaration(root.location + "#/", dialectDomainElement)

        dialectInstance.withEncodes(dialectDomainElement)
        if (ctx.declarations.declarables().nonEmpty)
          dialectInstance.withDeclares(ctx.declarations.declarables())
        if (references.baseUnitReferences().nonEmpty)
          dialectInstance.withReferences(references.baseUnitReferences())
        if (ctx.nestedDialects.nonEmpty)
          dialectInstance.withGraphDependencies(ctx.nestedDialects.map(nd => nd.location().getOrElse(nd.id)))
        Some(dialectInstance)

      case _ => None

    }

    // resolve unresolved references
    ctx.futureDeclarations.resolve()

    document
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
    }
    else {
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
            val declarationsId = root.location + "#" + normalizedPath.getOrElse("/") + name.urlComponentEncoded
            entry.value.as[YMap].entries.foreach { declarationEntry =>
              val declarationName = declarationEntry.key.as[YScalar].text
              val id              = pathSegment(declarationsId, List(declarationName))
              parseNode(declarationsId,
                        id,
                        declarationEntry.value,
                        nodeMapping,
                        givenAnnotations = Some(Annotations(declarationEntry))) match {
                case Some(node) =>
                  // lookup by ref name

                  node.set(DialectDomainElementModel.DeclarationName,
                           AmfScalar(declarationName, Annotations(declarationEntry.key)),
                           Annotations(declarationEntry.key))
                  ctx.declarations.registerDialectDomainElement(declarationEntry.key, node)
                  // lookup by JSON pointer, absolute URI
                  ctx.registerJsonPointerDeclaration(id, node)
                case _ =>
                  ctx.eh.violation(DialectError,
                                   id,
                                   s"Cannot parse declaration for node with key '$declarationName'",
                                   entry.value)
              }
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

  protected def parseEncoded(dialectInstance: EncodesModel): Option[DialectDomainElement] = {
    Option(ctx.dialect.documents()) flatMap { documents: DocumentsModel =>
      Option(documents.root()) flatMap { mapping =>
        ctx.findNodeMapping(mapping.encoded().value()) match {
          case Some(nodeMapping) =>
            val path = dialectInstance.id + "#"
            val additionalKey =
              if (documents.keyProperty().value()) {
                Some(ctx.dialect.name().value())
              }
              else None
            parseNode(path,
                      encodedElementDefaultId(dialectInstance),
                      map,
                      nodeMapping,
                      rootNode = true,
                      givenAnnotations = None,
                      additionalKey = additionalKey)
          case _ => None
        }
      }
    }
  }

  def checkClosedNode(id: String,
                      nodetype: String,
                      entries: Map[YNode, YNode],
                      mapping: NodeMapping,
                      ast: YPart,
                      rootNode: Boolean,
                      additionalKey: Option[String]): Unit = {
    val rootProps: Set[String] = if (rootNode) {
      ctx.rootProps
    }
    else {
      Set[String]()
    }
    val props = mapping.propertiesMapping().map(_.name().value()).toSet.union(rootProps)
    val inNode = entries.keys
      .map(_.value.asInstanceOf[YScalar].text)
      .filter(p => !p.startsWith("$") && !p.startsWith("(") && !p.startsWith("x-"))
      .filterNot(additionalKey.contains)
      .toSet
    val outside = inNode.diff(props)
    if (outside.nonEmpty) {
      outside.foreach { prop =>
        val posAst = entries.find(_._1.toString == prop).map(_._2).getOrElse(ast)
        ctx.closedNodeViolation(id, prop, nodetype, posAst)
      }
    }
  }

  protected def parseNode(path: String,
                          defaultId: String,
                          ast: YNode,
                          mappable: NodeMappable,
                          additionalProperties: Map[String, Any] = Map(),
                          rootNode: Boolean = false,
                          givenAnnotations: Option[Annotations],
                          additionalKey: Option[String] = None): Option[DialectDomainElement] = {
    val result = ast.tagType match {
      case YType.Map =>
        val nodeMap = ast.as[YMap]
        dispatchNodeMap(nodeMap) match {
          case "$ref" =>
            resolveJSONPointer(nodeMap, mappable, defaultId).map { ref =>
              ref.annotations += JsonPointerRef()
              mappable match {
                case m: NodeMapping => ref.withDefinedBy(m)
                case _              => // ignore
              }
              ref
            }
          case "$include" =>
            resolveLink(ast, mappable, defaultId, givenAnnotations).map { link =>
              link.annotations += RefInclude()
              link
            }
          case _ =>
            mappable match {
              case mapping: NodeMapping =>
                val node: DialectDomainElement =
                  DialectDomainElement(givenAnnotations.getOrElse(Annotations(nodeMap))).withDefinedBy(mapping)
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
                checkClosedNode(finalId, mapping.id, nodeMap.map, mapping, nodeMap, rootNode, additionalKey)
                Some(node)

              case unionMapping: UnionNodeMapping =>
                parseObjectUnion(defaultId, Seq(path), ast, unionMapping, additionalProperties)
            }

        }

      case YType.Str => resolveLink(ast, mappable, defaultId, givenAnnotations)
      case YType.Include          => resolveLink(ast, mappable, defaultId, givenAnnotations)
      case YType.Null =>
        emptyElement(defaultId, ast, mappable, givenAnnotations)
      case _ =>
        ctx.eh.violation(DialectError, defaultId, "Cannot parse AST node for node in dialect instance", ast)
        None
    }
    // if we are parsing a patch document we mark the node as abstract
    result match {
      case Some(node) =>
        if (ctx.isPatch) node.withAbstract(true)
        mappable match {
          case mapping: NodeMapping =>
            node
              .withDefinedBy(mapping)
              .withInstanceTypes(Seq(mapping.nodetypeMapping.option(), Some(mapping.id)).collect { case Some(t) => t })
          case _ => // ignore
        }
      case other => other
    }
    result
  }

  private def emptyElement(defaultId: String, ast: YNode, mappable: NodeMappable, givenAnnotations: Option[Annotations]) = {
    val mappings = mappable match {
      case m: NodeMapping => Seq(m.nodetypeMapping.value(), mappable.id)
      case _ => Seq(mappable.id)
    }
    val element = DialectDomainElement(givenAnnotations.getOrElse(Annotations(ast)))
      .withId(defaultId)
      .withInstanceTypes(mappings)
    ctx.eh.warning(DialectError,
      defaultId,
      s"Empty map: ${mappings.head}",
      ast)
    Some(element)
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
      case _ =>
        ctx.eh.violation(DialectError, id, s"Unknown type of node property ${property.id}", propertyEntry)
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
            AMLPlugin().registry.findNode(dialectNode) match {
              case Some((dialect, nodeMapping)) =>
                ctx.nestedDialects ++= Seq(dialect)
                ctx.withCurrentDialect(dialect) {
                  parseNestedNode(id, nestedObjectId, propertyEntry.value, nodeMapping) match {
                    case Some(dialectDomainElement) =>
                      node.setObjectField(property, dialectDomainElement, propertyEntry.value)
                    case None => // ignore
                  }
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

  protected def findHashProperties(propertyMapping: PropertyMapping,
                                   propertyEntry: YMapEntry): Option[(String, Any)] = {
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
        DiscriminatorField(propertyName,entry.value.as[YScalar].text)
      }
    }
  }

  protected def parseObjectUnion[T <: DomainElement](
      defaultId: String,
      path: Seq[String],
      ast: YNode,
      unionMapping: NodeWithDiscriminator[T],
      additionalProperties: Map[String, Any] = Map()): Option[DialectDomainElement] = {

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
        dispatchNodeMap(nodeMap) match {
          case "$include" =>
            resolveLinkUnion(ast, allPossibleMappings, defaultId).map { link =>
              link.annotations += RefInclude()
              link
            }
          case "$ref" =>
            resolveJSONPointerUnion(nodeMap, allPossibleMappings, defaultId).map { ref =>
              ref.annotations += JsonPointerRef()
              ref
            }
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
              None
            }
            else if (mappings.size == 1) {
              val node: DialectDomainElement = DialectDomainElement(nodeMap).withDefinedBy(mappings.head)
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
              Some(node)
            }
            else {
              ctx.eh.violation(
                  DialectAmbiguousRangeSpecification,
                  defaultId,
                  None, // Some(property.nodePropertyMapping().value()),
                  s"Ambiguous node, please provide a type disambiguator. Nodes ${mappings.map(_.id).mkString(",")} have been found compatible, only one is allowed",
                  map
              )
              None
            }
        }

      case YType.Str | YType.Include => // here the mapping information is explicit in the fragment/declaration mapping
        resolveLinkUnion(ast, allPossibleMappings, defaultId)

      case _ =>
        ctx.eh.violation(InvalidUnionType, defaultId, "Cannot parse AST for union node mapping", ast)
        None
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
        parseObjectUnion(nestedObjectId, Seq(path), propertyEntry.value, property, additionalProperties) match {
          case Some(parsedRange) => node.setObjectField(property, parsedRange, propertyEntry.value)
          case None              => // ignore
        }
      case range: Seq[String] if range.size == 1 =>
        ctx.dialect.declares.find(_.id == range.head) match {
          case Some(nodeMapping: NodeMappable) =>
            parseNestedNode(id, nestedObjectId, propertyEntry.value, nodeMapping, additionalProperties) match {
              case Some(dialectDomainElement) =>
                node.setObjectField(property, dialectDomainElement, propertyEntry.value)
              case _ => // ignore
            }
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
          parseObjectUnion(nestedObjectId, path, keyEntry.value, property, keyValueAdditionalProperties)
        case range: Seq[String] if range.size == 1 =>
          ctx.dialect.declares.find(_.id == range.head) match {
            case Some(nodeMapping: NodeMappable) if keyEntry.value.tagType != YType.Null =>
              parseNestedNode(id, nestedObjectId, keyEntry.value, nodeMapping, keyAdditionalProperties)
            case _ => None
          }
        case _ => None
      }
      parsedNode match {
        case Some(dialectDomainElement) => Some(checkHashProperties(dialectDomainElement, property, keyEntry))
        case None                       => None
      }
    }
    node.setObjectField(property, nested.collect { case Some(node: DialectDomainElement) => node }, propertyEntry.value)
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
          propertyEntry.value.as[YMap].entries map { pair: YMapEntry =>
            val nestedId = id + "/" + propertyEntry.key.as[YScalar].text.urlComponentEncoded + "/" + pair.key
              .as[YScalar]
              .text
              .urlComponentEncoded
            val nestedNode = DialectDomainElement(Annotations(pair))
              .withId(nestedId)
              .withDefinedBy(nodeMapping)
              .withInstanceTypes(Seq(nodeMapping.nodetypeMapping.value(), nodeMapping.id))
            try {
              nestedNode.set(Field(Str, ValueType(propertyKeyMapping.get)),
                             AmfScalar(pair.key.as[YScalar].text),
                             Annotations(pair.key))
              nestedNode.set(Field(Str, ValueType(propertyValueMapping.get)),
                             AmfScalar(pair.value.as[YScalar].text),
                             Annotations(pair.value))
            } catch {
              case e: UnknownMapKeyProperty =>
                ctx.eh.violation(DialectError, e.id, s"Cannot find mapping for key map property ${e.id}", pair)
            }
            Some(nestedNode)
          } collect { case Some(elem: DialectDomainElement) => elem }
        case _ =>
          ctx.eh.violation(
              DialectError,
              id,
              s"Cannot find mapping for property range of mapValue property: ${property.objectRange().head.value()}",
              propertyEntry
          )
          Nil
      }

      node.setObjectField(property, nested, propertyEntry.key)

    }
    else {
      ctx.eh.violation(DialectError,
                       id,
                       s"Both 'mapKey' and 'mapValue' are mandatory in a map pair property mapping",
                       propertyEntry)
    }
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

    val res = entries.zipWithIndex.map {
      case (elementNode, nextElem) =>
        val path           = List(propertyEntry.key.as[YScalar].text, nextElem.toString)
        val nestedObjectId = pathSegment(id, path)
        property.nodesInRange match {
          case range: Seq[String] if range.size > 1 =>
            parseObjectUnion(nestedObjectId, path, elementNode, property, additionalProperties) map { parsed =>
              checkDuplicated(parsed, elementNode, idsMap)
              parsed
            }
          case range: Seq[String] if range.size == 1 =>
            ctx.dialect.declares.find(_.id == range.head) match {
              case Some(nodeMapping: NodeMappable) =>
                parseNestedNode(id, nestedObjectId, elementNode, nodeMapping, additionalProperties) match {
                  case Some(dialectDomainElement) =>
                    checkDuplicated(dialectDomainElement, elementNode, idsMap)
                    Some(dialectDomainElement)
                  case None => None
                }
              case _ => None
            }
          case _ => None
        }
    }
    val elems: Seq[DialectDomainElement] = res.collect { case Some(x: DialectDomainElement) => x }
    node.setObjectField(property, elems, propertyEntry.value)
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
      }
      else {
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
                                additionalProperties: Map[String, Any] = Map()): Option[DialectDomainElement] =
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
                            givenAnnotations: Option[Annotations]): Option[DialectDomainElement] = {
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
        Some(linkedNode)
      case (text: String, _) =>
        val linkedNode = DialectDomainElement(givenAnnotations.getOrElse(Annotations(ast)))
          .withId(id)
          .withInstanceTypes(Seq(mapping.id))
        linkedNode.unresolved(text, givenAnnotations.flatMap(_.find(classOf[SourceAST])).map(_.ast).getOrElse(ast))
        Some(linkedNode)
    }
  }

  protected def resolveLinkUnion(ast: YNode,
                                 allPossibleMappings: Seq[NodeMapping],
                                 id: String): Some[DialectDomainElement] = {
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
        Some(linkedNode)
      case (text: String, _) =>
        val linkedNode = DialectDomainElement(map).withId(id)
        linkedNode.unresolved(text, map)
        Some(linkedNode)
    }
  }

  protected def resolveJSONPointerUnion(map: YMap,
                                        allPossibleMappings: Seq[NodeMapping],
                                        id: String): Option[DialectDomainElement] = {
    val entry   = map.key("$ref").get
    val pointer = entry.value.as[String]
    val fullPointer = if (pointer.startsWith("#")) {
      root.location + pointer
    }
    else {
      pointer
    }
    ctx.findJsonPointer(fullPointer) map { node =>
      if (allPossibleMappings.exists(_.id == node.definedBy.id)) {
        node
          .link(pointer, Annotations(map))
          .asInstanceOf[DialectDomainElement]
          .withId(id)
      }
      else {
        val linkedNode = DialectDomainElement(map).withId(id)
        linkedNode.unresolved(fullPointer, map)
        linkedNode
      }
    } orElse {
      val linkedNode = DialectDomainElement(map).withId(id)
      linkedNode.unresolved(fullPointer, map)
      Some(linkedNode)
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
        AMLPlugin().registry.findNode(s.definedBy.id) match {
          case Some((dialect, _)) =>
            ctx.nestedDialects ++= Seq(dialect)
            val linkedExternal = s
              .link(text, Annotations(propertyEntry.value))
              .asInstanceOf[DialectDomainElement]
              .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            if (isRef) linkedExternal.annotations += RefInclude()
            node.setObjectField(mapping, linkedExternal, propertyEntry.value)
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
    }
    else {
      pointer
    }

    ctx.findJsonPointer(fullPointer) map { node =>
      node
        .link(pointer, Annotations(map))
        .asInstanceOf[DialectDomainElement]
        .withId(id)
    } match {
      case Some(s) =>
        AMLPlugin().registry.findNode(s.definedBy.id) match {
          case Some((dialect, _)) =>
            ctx.nestedDialects ++= Seq(dialect)
            val linkedExternal = s
              .link(pointer, Annotations(map))
              .asInstanceOf[DialectDomainElement]
              .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            node.setObjectField(mapping, linkedExternal, map)
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

  protected def scalarYType(entry: YMapEntry): Boolean = {
    entry.value.tagType match {
      case YType.Bool      => true
      case YType.Float     => true
      case YType.Str       => true
      case YType.Int       => true
      case YType.Timestamp => true
      case _               => false
    }
  }

  protected def generateNodeId(node: DialectDomainElement,
                               nodeMap: YMap,
                               path: Seq[String],
                               defaultId: String,
                               mapping: NodeMapping,
                               additionalProperties: Map[String, Any] = Map(),
                               rootNode: Boolean): String = {
    if (rootNode && Option(ctx.dialect.documents()).flatMap(_.selfEncoded().option()).getOrElse(false))
      defaultId // if this is self-encoded just reuse the dialectId computed and don't try to generate a different identifier
    else {
      if (nodeMap.key("$id").isDefined) {
        explicitNodeId(node, nodeMap, path, defaultId, mapping)
      }
      else if (mapping.idTemplate.nonEmpty) {
        templateNodeId(node, nodeMap, path, defaultId, mapping)
      }
      else if (mapping.primaryKey().nonEmpty) {
        primaryKeyNodeId(node, nodeMap, path, defaultId, mapping, additionalProperties)
      }
      else {
        defaultId
      }
    }
  }

  protected def explicitNodeId(node: DialectDomainElement,
                               nodeMap: YMap,
                               path: Seq[String],
                               defaultId: String,
                               mapping: NodeMapping): String = {
    // explicit $id
    val entry = nodeMap.key("$id").get
    val rawId = entry.value.as[YScalar].text
    val externalId = if (rawId.contains("://")) {
      rawId
    }
    else {
      (ctx.dialect.location().getOrElse(ctx.dialect.id).split("#").head + s"#$rawId").replace("##", "#")
    }
    node.annotations += CustomId()
    externalId
  }

  protected def templateNodeId(node: DialectDomainElement,
                               nodeMap: YMap,
                               path: Seq[String],
                               defaultId: String,
                               mapping: NodeMapping): String = {
    // template resolution
    var template = mapping.idTemplate.value()
    val regex    = "(\\{[^}]+\\})".r
    regex.findAllIn(template).foreach { varMatch =>
      val variable = varMatch.replace("{", "").replace("}", "")
      nodeMap.key(variable) match {
        case Some(entry) =>
          val value = entry.value.value.toString
          template = template.replace(varMatch, value)
        case None =>
          ctx.eh.violation(DialectError, node.id, s"Missing ID template variable '$variable' in node", nodeMap)
      }
    }
    if (template.contains("://"))
      template
    else if (template.startsWith("/"))
      root.location + "#" + template
    else if (template.startsWith("#"))
      root.location + template
    else {
      val pathLocation = (path ++ template.split("/")).mkString("/")
      if (pathLocation.startsWith(root.location) || pathLocation.contains("#")) {
        pathLocation
      }
      else {
        root.location + "#" + pathLocation
      }
    }
  }

  protected def primaryKeyNodeId(node: DialectDomainElement,
                                 nodeMap: YMap,
                                 path: Seq[String],
                                 defaultId: String,
                                 mapping: NodeMapping,
                                 additionalProperties: Map[String, Any] = Map()): String = {
    var allFound           = true
    var keyId: Seq[String] = Seq()
    mapping.primaryKey().foreach { key =>
      val propertyName = key.name().value()
      nodeMap.entries.find(_.key.as[YScalar].text == propertyName) match {
        case Some(entry) if scalarYType(entry) =>
          keyId = keyId ++ Seq(entry.value.value.asInstanceOf[YScalar].text)
        case _ =>
          additionalProperties.get(key.nodePropertyMapping().value()) match {
            case Some(v) => keyId = keyId ++ Seq(v.toString)
            case _ =>
              ctx.eh.violation(DialectError, node.id, s"Cannot find unique mandatory property '$propertyName'", nodeMap)
              allFound = false
          }
      }
    }
    if (allFound) { path.map(_.urlEncoded).mkString("/") + "/" + keyId.mkString("_").urlEncoded }
    else { defaultId }
  }
}