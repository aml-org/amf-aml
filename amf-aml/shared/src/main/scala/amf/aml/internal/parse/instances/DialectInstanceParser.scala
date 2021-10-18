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
import amf.aml.internal.parse.instances.ClosedInstanceNode.{checkClosedNode, checkNode, checkRootNode}
import amf.aml.internal.parse.instances.DialectInstanceParser.{
  computeParsingScheme,
  emptyElement,
  encodedElementDefaultId,
  findDeclarationsMap,
  pathSegment,
  typesFrom
}
import amf.aml.internal.parse.instances.finder.{IncludeFirstUnionElementFinder, JSONPointerUnionFinder}
import amf.aml.internal.parse.instances.parser.ObjectUnionParser.findCompatibleMapping
import amf.aml.internal.parse.instances.parser.{
  DialectExtensionParser,
  ExternalLinkGenerator,
  ExternalLinkPropertyParser,
  JSONPointerPropertyParser,
  KeyValuePropertyParser,
  LinkIncludePropertyParser,
  LiteralCollectionParser,
  LiteralValueParser,
  LiteralValueSetter,
  ObjectCollectionPropertyParser,
  ObjectUnionParser
}
import amf.aml.internal.validate.DialectValidations.{
  DialectAmbiguousRangeSpecification,
  DialectError,
  InvalidUnionType
}
import com.github.ghik.silencer.silent
import org.mulesoft.common.time.SimpleDateTime
import org.mulesoft.lexer.SourceLocation
import org.yaml.model._

import scala.collection.mutable

// TODO: needs further breakup of parts. This components of this class are untestable the current way.
// TODO: find out why all these methods are protected.
// TODO:

object DialectInstanceParser extends NodeMappableHelper {
  def typesFrom(mapping: NodeMapping): Seq[String] = {
    Seq(mapping.nodetypeMapping.option(), Some(mapping.id)).flatten
  }

  def encodedElementDefaultId(dialectInstance: EncodesModel)(implicit ctx: DialectInstanceContext): String =
    if (Option(ctx.dialect.documents()).flatMap(_.selfEncoded().option()).getOrElse(false))
      dialectInstance.location().getOrElse(dialectInstance.id)
    else
      dialectInstance.id + "#/encodes"

  @scala.annotation.tailrec
  def findDeclarationsMap(paths: List[String], map: YMap)(implicit ctx: DialectInstanceContext): Option[YMap] = {
    paths match {
      case Nil => Some(map)
      case head :: tail =>
        map.key(head) match {
          case Some(m) if m.value.tagType == YType.Map =>
            if (tail.nonEmpty) findDeclarationsMap(tail, m.value.as[YMap])
            else m.value.toOption[YMap]
          case Some(o) =>
            ctx.eh
              .violation(DialectError,
                         "",
                         s"Invalid node type for declarations path ${o.value.tagType.toString()}",
                         o.location)
            None
          case _ => None
        }
    }
  }

  def emptyElement(defaultId: String, ast: YNode, mappable: NodeMappable, givenAnnotations: Option[Annotations])(
      implicit ctx: DialectInstanceContext): DialectDomainElement = {
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
    ctx.eh.warning(DialectError, defaultId, s"Empty map: ${mappings.head}", ast.location)
    element
  }

  def computeParsingScheme(nodeMap: YMap): String =
    if (nodeMap.key("$include").isDefined)
      "$include"
    else if (nodeMap.key("$ref").isDefined)
      "$ref"
    else
      "inline"

  def pathSegment(parent: String, nexts: List[String]): String = {
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
}

class DialectInstanceParser(val root: Root)(implicit override val ctx: DialectInstanceContext)
    extends AnnotationsParser
    with DeclarationKeyCollector
    with JsonPointerResolver
    with InstanceNodeIdHandling {

  val map: YMap = root.parsed.asInstanceOf[SyamlParsedDocument].document.as[YMap]

  def parseDocument(): DialectInstance = {
    @silent("deprecated") // Silent can only be used in assignment expressions
    val dialectInstance: DialectInstance = DialectInstance(Annotations(map))
      .withLocation(root.location)
      .withId(root.location)
      .withProcessingData(DialectInstanceProcessingData().withTransformed(false).withDefinedBy(ctx.dialect.id))
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
    if (ctx.nestedDialects.nonEmpty) {
      dialectInstance.processingData.withGraphDependencies(ctx.nestedDialects.map(nd =>
        nd.location().getOrElse(nd.id)))
      @silent("deprecated") // Silent can only be used in assignment expressions
      val a = dialectInstance.withGraphDependencies(ctx.nestedDialects.map(nd => nd.location().getOrElse(nd.id)))
    }

    // resolve unresolved references
    ctx.futureDeclarations.resolve()

    dialectInstance
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
    ctx.eh.violation(DialectError, empty.id, msg, map.location)
    empty
  }

  private def checkNodeForAdditionalKeys(id: String,
                                         nodetype: String,
                                         entries: Map[YNode, YNode],
                                         mapping: NodeMapping,
                                         ast: YPart,
                                         rootNode: Boolean,
                                         additionalKey: Option[String]): Unit = {
    checkNode(id, nodetype, entries, mapping, ast, rootNode, additionalKey)
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
        computeParsingScheme(nodeMap) match {
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
      case YType.Null    => emptyElement(defaultId, ast, mappable, givenAnnotations)
      case _ =>
        ctx.eh.violation(DialectError, defaultId, "Cannot parse AST node for node in dialect instance", ast.location)
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
        ctx.eh.violation(DialectError, id, s"Unknown type of node property ${property.id}", propertyEntry.location)
    }
  }

  private def parseExternalLinkProperty(id: String,
                                        propertyEntry: YMapEntry,
                                        property: PropertyMapping,
                                        node: DialectDomainElement): Unit = {
    ExternalLinkPropertyParser.parse(id, propertyEntry, property, node, root, parseProperty)
  }

  protected def parseDialectExtension(id: String,
                                      propertyEntry: YMapEntry,
                                      property: PropertyMapping,
                                      node: DialectDomainElement): Unit = {
    DialectExtensionParser.parse(id, propertyEntry, property, node, root, parseNestedNode)
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

  protected def parseObjectUnion[T <: DomainElement](
      defaultId: String,
      path: Seq[String],
      ast: YNode,
      unionMapping: NodeWithDiscriminator[_ <: NodeWithDiscriminatorModel],
      additionalProperties: Map[String, Any] = Map()): DialectDomainElement = {

    ObjectUnionParser.parse(defaultId, path, ast, unionMapping, additionalProperties, root, map, parseProperty)
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
        node.withObjectField(property, parsedRange, Right(propertyEntry))
      case range: Seq[String] if range.size == 1 =>
        ctx.dialect.declares.find(_.id == range.head) match {
          case Some(nodeMapping: NodeMappable) =>
            val dialectDomainElement =
              parseNestedNode(id, nestedObjectId, propertyEntry.value, nodeMapping, additionalProperties)
            node.withObjectField(property, dialectDomainElement, Right(propertyEntry))
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
    node.withObjectCollectionProperty(property, nested.flatten, Right(propertyEntry))
  }

  protected def parseObjectPairProperty(id: String,
                                        propertyEntry: YMapEntry,
                                        property: PropertyMapping,
                                        node: DialectDomainElement): Unit =
    KeyValuePropertyParser.parse(id, propertyEntry, property, node)

  protected def parseObjectCollectionProperty(id: String,
                                              propertyEntry: YMapEntry,
                                              property: PropertyMapping,
                                              node: DialectDomainElement,
                                              additionalProperties: Map[String, Any] = Map()): Unit = {

    ObjectCollectionPropertyParser.parse(id,
                                         propertyEntry,
                                         property,
                                         node,
                                         additionalProperties,
                                         parseObjectUnion,
                                         parseNestedNode)
  }

  protected def parseLiteralValue(value: YNode, property: PropertyMapping, node: DialectDomainElement): Option[_] = {

    LiteralValueParser.parseLiteralValue(value, property, node)
  }

  // TODO: This should receive annotations instead of an entry. Unrelated concepts in the same method
  protected def setLiteralValue(entry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    val parsed = parseLiteralValue(entry.value, property, node)
    LiteralValueSetter.setLiteralValue(parsed, entry, property, node)
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
    LiteralCollectionParser.parse(propertyEntry, property, node)
  }

  protected def parseNestedNode(path: String,
                                id: String,
                                entry: YNode,
                                mapping: NodeMappable,
                                additionalProperties: Map[String, Any] = Map()): DialectDomainElement =
    parseNode(path, id, entry, mapping, additionalProperties, givenAnnotations = None)

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
        val loc = givenAnnotations.flatMap(_.find(classOf[SourceAST])).map(_.ast) match {
          case Some(n) => n.location
          case _       => ast.location
        }
        val linkedNode = DialectDomainElement(givenAnnotations.getOrElse(Annotations(ast)))
          .withId(id)
          .withInstanceTypes(Seq(mapping.id))
        linkedNode.unresolved(text, Nil, Some(loc))
        linkedNode
    }
  }

  protected def resolveLinkUnion(ast: YNode, allPossibleMappings: Seq[NodeMapping], id: String): DialectDomainElement = {
    IncludeFirstUnionElementFinder.find(ast, allPossibleMappings, id, map)
  }

  protected def resolveJSONPointerUnion(map: YMap,
                                        allPossibleMappings: Seq[NodeMapping],
                                        id: String): DialectDomainElement = {
    JSONPointerUnionFinder.find(map, allPossibleMappings, id, map)
  }
}
