package amf.plugins.document.vocabularies.parser.instances

import amf.core.Root
import amf.core.annotations.{Aliases, LexicalInformation}
import amf.core.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.model.domain.{Annotation, DomainElement}
import amf.core.parser.{
  Annotations,
  BaseSpecParser,
  EmptyFutureDeclarations,
  ErrorHandler,
  FutureDeclarations,
  ParsedReference,
  ParserContext,
  Reference,
  SearchScope,
  _
}
import amf.core.unsafe.PlatformSecrets
import amf.core.utils._
import amf.core.vocabulary.Namespace
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.annotations.{AliasesLocation, CustomId, JsonPointerRef, RefInclude}
import amf.plugins.document.vocabularies.model.document._
import amf.plugins.document.vocabularies.model.domain._
import amf.plugins.document.vocabularies.parser.common.{AnnotationsParser, SyntaxErrorReporter}
import amf.plugins.document.vocabularies.parser.vocabularies.VocabularyDeclarations
import amf.plugins.features.validation.ParserSideValidations.{
  DialectAmbiguousRangeSpecification,
  DialectError,
  InvalidUnionType
}
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model._

import scala.collection.mutable

trait NodeMappableHelper {

  def allNodeMappingIds(mapping: NodeMappable): Set[String] = mapping match {
    case nodeMapping: NodeMapping           => Set(nodeMapping.id)
    case unionNodeMapping: UnionNodeMapping => unionNodeMapping.objectRange().map(_.value()).toSet
  }

}

class DialectInstanceDeclarations(var dialectDomainElements: Map[String, DialectDomainElement] = Map(),
                                  errorHandler: Option[ErrorHandler],
                                  futureDeclarations: FutureDeclarations)
    extends VocabularyDeclarations(Map(), Map(), Map(), Map(), Map(), errorHandler, futureDeclarations)
    with NodeMappableHelper {

  /** Get or create specified library. */
  override def getOrCreateLibrary(alias: String): DialectInstanceDeclarations = {
    libraries.get(alias) match {
      case Some(lib: DialectInstanceDeclarations) => lib
      case _ =>
        val result =
          new DialectInstanceDeclarations(errorHandler = errorHandler, futureDeclarations = EmptyFutureDeclarations())
        libraries = libraries + (alias -> result)
        result
    }
  }

  def registerDialectDomainElement(name: String,
                                   dialectDomainElement: DialectDomainElement): DialectInstanceDeclarations = {
    dialectDomainElements += (name -> dialectDomainElement)
    if (!dialectDomainElement.isUnresolved) {
      futureDeclarations.resolveRef(name, dialectDomainElement)
    }
    this
  }

  def findDialectDomainElement(key: String,
                               nodeMapping: NodeMappable,
                               scope: SearchScope.Scope): Option[DialectDomainElement] = {
    val nodeMappingIds = allNodeMappingIds(nodeMapping)
    findForType(key, _.asInstanceOf[DialectInstanceDeclarations].dialectDomainElements, scope) collect {
      case dialectDomainElement: DialectDomainElement if nodeMappingIds.contains(dialectDomainElement.definedBy.id) =>
        dialectDomainElement
    }
  }

  def findAnyDialectDomainElement(key: String, scope: SearchScope.Scope): Option[DialectDomainElement] = {
    findForType(key, _.asInstanceOf[DialectInstanceDeclarations].dialectDomainElements, scope) collect {
      case dialectDomainElement: DialectDomainElement => dialectDomainElement
    }
  }

  override def declarables: Seq[DialectDomainElement] = dialectDomainElements.values.toSet.toSeq
}

class DialectInstanceContext(var dialect: Dialect,
                             private val wrapped: ParserContext,
                             private val ds: Option[DialectInstanceDeclarations] = None)
    extends ParserContext(wrapped.rootContextDocument, wrapped.refs, wrapped.futureDeclarations, wrapped.parserCount)
    with SyntaxErrorReporter {

  var isPatch: Boolean                                           = false
  var nestedDialects: Seq[Dialect]                               = Nil
  val libraryDeclarationsNodeMappings: Map[String, NodeMappable] = parseDeclaredNodeMappings("library")
  val rootDeclarationsNodeMappings: Map[String, NodeMappable]    = parseDeclaredNodeMappings("root")

  globalSpace = wrapped.globalSpace
  reportDisambiguation = wrapped.reportDisambiguation

  def forPatch(): DialectInstanceContext = {
    isPatch = true
    this
  }

  def registerJsonPointerDeclaration(pointer: String, declared: DialectDomainElement) =
    globalSpace.update(pointer, declared)

  def findJsonPointer(pointer: String): Option[DialectDomainElement] = globalSpace.get(pointer) match {
    case Some(e: DialectDomainElement) => Some(e)
    case _                             => None
  }

  val declarations: DialectInstanceDeclarations =
    ds.getOrElse(new DialectInstanceDeclarations(errorHandler = Some(this), futureDeclarations = futureDeclarations))

  def withCurrentDialect[T](tmpDialect: Dialect)(k: => T) = {
    val oldDialect = dialect
    dialect = tmpDialect
    val res = k
    dialect = oldDialect
    res
  }

  protected def parseDeclaredNodeMappings(documentType: String): Map[String, NodeMappable] = {
    val declarations: Seq[(String, NodeMappable)] = Option(dialect.documents())
      .flatMap { documents =>
        // document mappings for root and libraries, everything that declares something
        val documentMappings: Option[DocumentMapping] = if (documentType == "root") {
          Option(documents.root())
        } else {
          Option(documents.library())
        }
        documentMappings.map { mapping =>
          mapping.declaredNodes() map { declaration: PublicNodeMapping =>
            findNodeMapping(declaration.mappedNode().value()) map { nodeMapping =>
              (declaration.name().value(), nodeMapping)
            }
          } collect { case Some(res: (String, NodeMappable)) => res }
        }
      }
      .getOrElse(Nil)

    declarations.foldLeft(Map[String, NodeMappable]()) {
      case (acc, (name, mapping)) =>
        acc + (name -> mapping)
    }
  }

  def findNodeMapping(mappingId: String): Option[NodeMappable] = {
    dialect.declares.collectFirst {
      case mapping: NodeMappable if mapping.id == mappingId => mapping
    }
  }

  private def isInclude(node: YNode) = node.tagType == YType.Include

  private def isIncludeMap(node: YNode): Boolean =
    node.value.isInstanceOf[YMap] && node.as[YMap].key("$include").isDefined

  def link(node: YNode): Either[String, YNode] = {
    node match {
      case _ if isInclude(node)    => Left(node.as[YScalar].text)
      case _ if isIncludeMap(node) => Left(node.as[YMap].key("$include").get.value.as[String])
      case _                       => Right(node)
    }
  }
}

case class ReferenceDeclarations(references: mutable.Map[String, Any] = mutable.Map())(
    implicit ctx: DialectInstanceContext) {
  def +=(alias: String, unit: BaseUnit): Unit = {
    references += (alias -> unit)
    // useful for annotations
    if (unit.isInstanceOf[Vocabulary]) ctx.declarations.registerUsedVocabulary(alias, unit.asInstanceOf[Vocabulary])
    // register declared units properly
    unit match {
      case m: DeclaresModel =>
        val library = ctx.declarations.getOrCreateLibrary(alias)
        m.declares.foreach {
          case dialectElement: DialectDomainElement =>
            val localName = dialectElement.localRefName
            library.registerDialectDomainElement(localName, dialectElement)
            ctx.futureDeclarations.resolveRef(s"$alias.$localName", dialectElement)
          case decl => library += decl
        }
      case f: DialectInstanceFragment =>
        ctx.declarations.fragments += (alias -> FragmentRef(f.encodes, f.location()))
    }
  }

  def +=(external: External): Unit = {
    references += (external.alias.value()                 -> external)
    ctx.declarations.externals += (external.alias.value() -> external)
  }

  def baseUnitReferences(): Seq[BaseUnit] =
    references.values.toSet.filter(_.isInstanceOf[BaseUnit]).toSeq.asInstanceOf[Seq[BaseUnit]]
}

case class DialectInstanceReferencesParser(dialectInstance: BaseUnit, map: YMap, references: Seq[ParsedReference])(
    implicit ctx: DialectInstanceContext) {

  def parse(location: String): ReferenceDeclarations = {
    val result = ReferenceDeclarations()
    parseLibraries(dialectInstance, result, location)
    parseExternals(result, location)
    references.foreach {
      case ParsedReference(f: DialectInstanceFragment, origin: Reference, None) => result += (origin.url, f)
      case _                                                                    =>
    }
    if (ctx.isPatch) {
      references.foreach {
        case ParsedReference(f: DialectInstance, origin: Reference, None) => result += (origin.url, f)
        case _                                                            =>
      }
    }

    result
  }

  private def target(url: String): Option[BaseUnit] =
    references.find(r => r.origin.url.equals(url)).map(_.unit)

  private def parseLibraries(dialectInstance: BaseUnit, result: ReferenceDeclarations, id: String): Unit = {
    val parsedLibraries: mutable.Set[String] = mutable.Set()
    map.key(
      "uses",
      entry => {
        val annotation: Annotation =
          AliasesLocation(
            Annotations(entry.key).find(classOf[LexicalInformation]).map(_.range.start.line).getOrElse(0))
        dialectInstance.annotations += annotation
        entry.value
          .as[YMap]
          .entries
          .foreach(e => {
            val alias: String = e.key.as[YScalar].text
            val url: String   = library(e)
            target(url).foreach {
              case module: DeclaresModel =>
                parsedLibraries += url
                collectAlias(dialectInstance, alias -> (module.id, url))
                result += (alias, module)
              case other =>
                ctx.violation(DialectError, id, s"Expected vocabulary module but found: '$other'", e) // todo Uses should only reference modules...
            }
          })
      }
    )
    // Parsing $refs to libraries
    references.foreach {
      case ParsedReference(lib: DialectInstanceLibrary, _, _)
          if !parsedLibraries.contains(lib.location().getOrElse(lib.id)) =>
        result += (lib.id, lib)
      case _ => // ignore
    }
  }

  private def library(e: YMapEntry): String = e.value.tagType match {
    case YType.Include => e.value.as[YScalar].text
    case YType.Map if e.value.as[YMap].key("$include").isDefined =>
      e.value.as[YMap].key("$include").get.value.as[String]
    case _ => e.value
  }

  private def parseExternalEntry(result: ReferenceDeclarations, entry: YMapEntry): Unit = {
    entry.value
      .as[YMap]
      .entries
      .foreach(e => {
        val alias: String = e.key.as[YScalar].text
        val base: String  = e.value
        val external      = External()
        result += external.withAlias(alias).withBase(base)
      })
  }
  private def parseExternals(result: ReferenceDeclarations, id: String): Unit = {
    map.key(
      "external",
      entry => parseExternalEntry(result, entry)
    )

    map.key(
      "$external",
      entry => parseExternalEntry(result, entry)
    )
  }

  private def collectAlias(aliasCollectorUnit: BaseUnit,
                           alias: (Aliases.Alias, (Aliases.FullUrl, Aliases.RelativeUrl))): BaseUnit = {
    aliasCollectorUnit.annotations.find(classOf[Aliases]) match {
      case Some(aliases) =>
        aliasCollectorUnit.annotations.reject(_.isInstanceOf[Aliases])
        aliasCollectorUnit.add(aliases.copy(aliases = aliases.aliases + alias))
      case None => aliasCollectorUnit.add(Aliases(Set(alias)))
    }
  }
}

class DialectInstanceParser(root: Root)(implicit override val ctx: DialectInstanceContext)
    extends BaseSpecParser
    with PlatformSecrets
    with AnnotationsParser
    with NodeMappableHelper {

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
        if (ctx.declarations.declarables.nonEmpty)
          dialectInstance.withDeclares(ctx.declarations.declarables)
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

  def parsePatch(): Option[DialectInstancePatch] = {
    parseDocument() match {
      case Some(dialectInstance) =>
        val patch = DialectInstancePatch(dialectInstance.fields, dialectInstance.annotations)
        patch.withId(dialectInstance.id)
        Some(checkTarget(patch))
      case _ =>
        None
    }
  }

  def parseFragment(): Option[DialectInstanceFragment] = {
    val dialectInstanceFragment: DialectInstanceFragment = DialectInstanceFragment(Annotations(map))
      .withLocation(root.location)
      .withId(root.location)
      .withDefinedBy(ctx.dialect.id)

    DialectInstanceReferencesParser(dialectInstanceFragment, map, root.references).parse(root.location)

    if (ctx.declarations.externals.nonEmpty)
      dialectInstanceFragment.withExternals(ctx.declarations.externals.values.toSeq)

    parseEncodedFragment(dialectInstanceFragment) match {
      case Some(dialectDomainElement) =>
        // registering JSON pointer
        ctx.registerJsonPointerDeclaration(root.location + "#/", dialectDomainElement)

        Some(dialectInstanceFragment.withEncodes(dialectDomainElement))
      case _ => None
    }
  }

  def parseLibrary(): Option[DialectInstanceLibrary] = {
    val dialectInstance: DialectInstanceLibrary = DialectInstanceLibrary(Annotations(map))
      .withLocation(root.location)
      .withId(root.location)
      .withDefinedBy(ctx.dialect.id)

    parseDeclarations("library")

    val references =
      DialectInstanceReferencesParser(dialectInstance, map, root.references)
        .parse(dialectInstance.location().getOrElse(dialectInstance.id))

    if (ctx.declarations.externals.nonEmpty)
      dialectInstance.withExternals(ctx.declarations.externals.values.toSeq)

    if (ctx.declarations.declarables.nonEmpty)
      dialectInstance.withDeclares(ctx.declarations.declarables)

    if (references.baseUnitReferences().nonEmpty)
      dialectInstance.withReferences(references.baseUnitReferences())

    // resolve unresolved references
    ctx.futureDeclarations.resolve()

    Some(dialectInstance)
  }

  private def findDeclarationsMap(paths: List[String], map: YMap): Option[YMap] = {
    paths match {
      case Nil => Some(map)
      case head :: tail =>
        map.key(head) match {
          case Some(m) if m.value.tagType == YType.Map =>
            if (tail.nonEmpty) findDeclarationsMap(tail, m.value.as[YMap])
            else m.value.toOption[YMap]
          case Some(o) =>
            ctx.violation(DialectError,
                          "",
                          s"Invalid node type for declarations path ${o.value.tagType.toString()}",
                          o)
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

    val pathOption = ctx.dialect.documents().declarationsPath().option()
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
              parseNode(declarationsId, id, declarationEntry.value, nodeMapping, Map()) match {
                case Some(node) =>
                  // lookup by ref name
                  node.withDeclarationName(declarationName)
                  ctx.declarations.registerDialectDomainElement(declarationEntry.key, node)
                  // lookup by JSON pointer, absolute URI
                  ctx.registerJsonPointerDeclaration(id, node)
                case _ =>
                  ctx.violation(DialectError,
                                id,
                                s"Cannot parse declaration for node with key '$declarationName'",
                                entry.value)
              }
            }
          }
      }
    }
  }

  protected def encodedElementDefaultId(dialectInstance: EncodesModel): String = {
    if (ctx.dialect.documents().selfEncoded().option().getOrElse(false)) {
      dialectInstance.location().getOrElse(dialectInstance.id)
    } else {
      dialectInstance.id + "#/"
    }
  }

  protected def parseEncoded(dialectInstance: EncodesModel): Option[DialectDomainElement] = {
    Option(ctx.dialect.documents()) flatMap { documents: DocumentsModel =>
      Option(documents.root()) flatMap { mapping =>
        ctx.findNodeMapping(mapping.encoded().value()) match {
          case Some(nodeMapping) =>
            val path = dialectInstance.id + "#"
            parseNode(path, encodedElementDefaultId(dialectInstance), map, nodeMapping, Map(), rootNode = true)
          case _ => None
        }
      }
    }
  }

  protected def parseEncodedFragment(dialectInstanceFragment: DialectInstanceFragment): Option[DialectDomainElement] = {
    Option(ctx.dialect.documents()) flatMap { documents: DocumentsModel =>
      documents.fragments().find { documentMapping =>
        root.parsed
          .asInstanceOf[SyamlParsedDocument]
          .comment
          .get
          .metaText
          .replace(" ", "")
          .contains(documentMapping.documentName().value())
      } match {
        case Some(documentMapping) =>
          ctx.findNodeMapping(documentMapping.encoded().value()) match {
            case Some(nodeMapping) =>
              val path = dialectInstanceFragment.id + "#"
              parseNode(path, path + "/", map, nodeMapping, Map())
            case _ => None
          }
        case None => None
      }
    }
  }

  protected def parseNode(path: String,
                          defaultId: String,
                          ast: YNode,
                          mappable: NodeMappable,
                          additionalProperties: Map[String, Any],
                          rootNode: Boolean = false): Option[DialectDomainElement] = {
    val result = ast.tagType match {
      case YType.Map =>
        val nodeMap = ast.as[YMap]
        dispatchNodeMap(nodeMap) match {
          case "$ref" =>
            resolveJSONPointer(nodeMap, mappable, defaultId).map { ref =>
              ref.annotations += JsonPointerRef()
              ref
            }
          case "$include" =>
            resolveLink(ast, mappable, defaultId).map { link =>
              link.annotations += RefInclude()
              link
            }
          case _ =>
            mappable match {
              case mapping: NodeMapping =>
                val node: DialectDomainElement = DialectDomainElement(nodeMap).withDefinedBy(mapping)
                val finalId =
                  generateNodeId(node, nodeMap, Seq(path), defaultId, mapping, additionalProperties, rootNode)
                node.withId(finalId)
                node.withInstanceTypes(Seq(mapping.nodetypeMapping.value(), mapping.id))
                parseAnnotations(nodeMap, node, ctx.declarations)
                mapping.propertiesMapping().foreach { propertyMapping =>
                  val propertyName = propertyMapping.name().value()
                  nodeMap.entries.find(_.key.as[YScalar].text == propertyName) match {
                    case Some(entry) =>
                      val nestedId =
                        if (ctx.dialect.documents().selfEncoded().option().getOrElse(false) && rootNode)
                          defaultId + "#/"
                        else defaultId
                      parseProperty(nestedId, entry, propertyMapping, node)
                    case None => // ignore
                  }
                }
                Some(node)

              case unionMapping: UnionNodeMapping =>
                parseObjectUnion(defaultId, Seq(path), ast, unionMapping, additionalProperties)
            }

        }

      case YType.Str     => resolveLink(ast, mappable, defaultId)
      case YType.Include => resolveLink(ast, mappable, defaultId)
      case _ =>
        ctx.violation(DialectError, defaultId, "Cannot parse AST node for node in dialect instance", ast)
        None
    }
    // if we are parsing a patch document we mark the node as abstract
    result match {
      case Some(node) if ctx.isPatch =>
        Some(node.withAbstract(true))
      case other => other
    }
  }

  protected def parseProperty(id: String,
                              propertyEntry: YMapEntry,
                              property: PropertyMapping,
                              node: DialectDomainElement): Unit = {
    property.classification() match {
      case ExtensionPointProperty    => parseDialectExtension(id, propertyEntry, property, node)
      case LiteralProperty           => parseLiteralProperty(id, propertyEntry, property, node)
      case LiteralPropertyCollection => parseLiteralCollectionProperty(id, propertyEntry, property, node)
      case ObjectProperty            => parseObjectProperty(id, propertyEntry, property, node, Map())
      case ObjectPropertyCollection  => parseObjectCollectionProperty(id, propertyEntry, property, node, Map())
      case ObjectMapProperty         => parseObjectMapProperty(id, propertyEntry, property, node, Map())
      case ObjectPairProperty        => parseObjectPairProperty(id, propertyEntry, property, node)
      case _ =>
        ctx.violation(DialectError, id, s"Unknown type of node property ${property.id}", propertyEntry)
    }
  }

  protected def parseDialectExtension(id: String,
                                      propertyEntry: YMapEntry,
                                      property: PropertyMapping,
                                      node: DialectDomainElement): Unit = {
    propertyEntry.value.tagType match {
      case YType.Str | YType.Include =>
        resolveLinkProperty(propertyEntry, property, id, node)
      case YType.Map =>
        val map = propertyEntry.value.as[YMap]
        map.key("$dialect") match {
          case Some(nested) if nested.value.tagType == YType.Str =>
            val dialectNode = nested.value.as[YScalar].text
            // TODO: resolve dialect node URI to absolute normalised URI
            AMLPlugin.registry.findNode(dialectNode) match {
              case Some((dialect, nodeMapping)) =>
                ctx.nestedDialects ++= Seq(dialect)
                ctx.withCurrentDialect(dialect) {
                  val nestedObjectId = pathSegment(id, List(propertyEntry.key.as[YScalar].text))
                  parseNestedNode(id, nestedObjectId, propertyEntry.value, nodeMapping, Map()) match {
                    case Some(dialectDomainElement) =>
                      node.setObjectField(property, dialectDomainElement, propertyEntry.value)
                    case None => // ignore
                  }
                }
              case None =>
                ctx.violation(DialectError,
                              id,
                              s"Cannot find dialect for nested anyNode mapping $dialectNode",
                              nested.value)
            }
          case None =>
            dispatchNodeMap(map) match {
              case "$include" =>
                val includeEntry = map.key("$include").get
                resolveLinkProperty(includeEntry, property, id, node, isRef = true)
              case "$ref" =>
                resolveJSONPointerProperty(map, property, id, node)
              case _ =>
                ctx.violation(DialectError, id, "$dialect key without string value or link", map)
            }
        }
    }
  }

  protected def findHashProperties(propertyMapping: PropertyMapping, propertyEntry: YMapEntry): Option[(String, Any)] = {
    propertyMapping.mapKeyProperty().option() match {
      case Some(propId) => Some((propId, propertyEntry.key.as[YScalar].text))
      case None         => None
    }
  }

  protected def checkHashProperties(node: DialectDomainElement,
                                    propertyMapping: PropertyMapping,
                                    propertyEntry: YMapEntry): DialectDomainElement = {
    // TODO: check if the node already has a value and that it matches (maybe coming from a declaration)
    propertyMapping.mapKeyProperty().option() match {
      case Some(propId) =>
        try {
          node.setMapKeyField(propId, propertyEntry.key.as[YScalar].text, propertyEntry.key)
        } catch {
          case e: UnknownMapKeyProperty =>
            ctx.violation(DialectError, e.id, s"Cannot find mapping for key map property ${e.id}")
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
                                      mapProperties: Seq[String]): Seq[NodeMapping] = {
    discriminator match {
      // Using explicit discriminator
      case Some(propertyName) =>
        val explicitMapping = nodeMap.entries.find(_.key.as[YScalar].text == propertyName).flatMap { entry =>
          discriminatorMapping.get(entry.value.as[YScalar].text)
        }
        explicitMapping match {
          case Some(nodeMapping) => Seq(nodeMapping)
          case None =>
            ctx.violation(DialectError,
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
            mapping.propertiesMapping().filter(pm => !mapProperties.contains(pm.nodePropertyMapping().value()))
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

  protected def parseObjectUnion[T <: DomainElement](
      defaultId: String,
      path: Seq[String],
      ast: YNode,
      mappableWithDiscriminator: NodeWithDiscriminator[T],
      additionalProperties: Map[String, Any]): Option[DialectDomainElement] = {
    // potential node range based in the objectRange
    val unionMappings = mappableWithDiscriminator.objectRange().map { nodeMappingId =>
      ctx.dialect.declares.find(_.id == nodeMappingId.value()) match {
        case Some(nodeMapping) => Some(nodeMapping)
        case None =>
          ctx.violation(DialectError,
                        defaultId,
                        s"Cannot find mapping for property ${mappableWithDiscriminator.id} in union",
                        ast)
          None
      }
    } collect { case Some(mapping: NodeMapping) => mapping }
    // potential node range based in discriminators map
    val discriminatorsMapping =
      Option(mappableWithDiscriminator.typeDiscriminator()).getOrElse(Map()).foldLeft(Map[String, NodeMapping]()) {
        case (acc, (alias, mappingId)) =>
          ctx.dialect.declares.find(_.id == mappingId) match {
            case Some(nodeMapping: NodeMapping) => acc + (alias -> nodeMapping)
            case _ =>
              ctx.violation(DialectError,
                            defaultId,
                            s"Cannot find mapping for property $mappingId in discriminator value '$alias' in union",
                            ast)
              acc
          }
      }
    // all possible mappings combining objectRange and type discriminator
    val allPossibleMappings = (unionMappings ++ discriminatorsMapping.values).distinct

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
            val mappings = findCompatibleMapping(defaultId,
                                                 unionMappings,
                                                 discriminatorsMapping,
                                                 mappableWithDiscriminator.typeDiscriminatorName().option(),
                                                 nodeMap,
                                                 additionalProperties.keys.toSeq)
            if (mappings.isEmpty) {
              ctx.violation(
                DialectAmbiguousRangeSpecification,
                defaultId,
                s"Ambiguous node in union range, found 0 compatible mappings from ${allPossibleMappings.size} mappings: [${allPossibleMappings.map(_.id).mkString(",")}]",
                ast
              )
              None
            } else if (mappings.size == 1) {
              val node: DialectDomainElement = DialectDomainElement(nodeMap).withDefinedBy(mappings.head)
              val finalId                    = generateNodeId(node, nodeMap, path, defaultId, mappings.head, additionalProperties, false)
              node.withId(finalId)
              var instanceTypes: Seq[String] = Nil
              mappings.foreach { mapping =>
                val beforeValues = node.literalProperties.size + node.objectCollectionProperties.size + node.objectProperties.size + node.mapKeyProperties.size
                mapping.propertiesMapping().foreach { propertyMapping =>
                  if (!node.containsProperty(propertyMapping)) {
                    val propertyName = propertyMapping.name().value()

                    nodeMap.entries.find(_.key.as[YScalar].text == propertyName) match {
                      case Some(entry) => parseProperty(finalId, entry, propertyMapping, node)
                      case None        => // ignore
                    }
                  }
                }
                val afterValues = node.literalProperties.size + node.objectCollectionProperties.size + node.objectProperties.size + node.mapKeyProperties.size
                if (afterValues != beforeValues) {
                  instanceTypes ++= Seq(mapping.nodetypeMapping.value())
                }
              }
              node.withInstanceTypes(instanceTypes ++ Seq(mappings.head.id))
              Some(node)
            } else {
              ctx.violation(
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
        ctx.violation(InvalidUnionType, defaultId, "Cannot parse AST for union node mapping", ast)
        None
    }
  }

  protected def parseObjectProperty(id: String,
                                    propertyEntry: YMapEntry,
                                    property: PropertyMapping,
                                    node: DialectDomainElement,
                                    additionalProperties: Map[String, Any]): Unit = {
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
                                       additionalProperties: Map[String, Any]): Unit = {
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
          val keyValueAdditionalProperties = property.mapValueProperty().option() match {
            case Some(mapValueProperty) => keyAdditionalProperties + (mapValueProperty -> "")
            case _                      => keyAdditionalProperties
          }
          // now we can parse the union with all the required properties to find the right node in the union
          parseObjectUnion(nestedObjectId, path, keyEntry.value, property, keyValueAdditionalProperties)
        case range: Seq[String] if range.size == 1 =>
          ctx.dialect.declares.find(_.id == range.head) match {
            case Some(nodeMapping: NodeMappable) =>
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
    node.setObjectField(property,
                        nested.collect { case Some(node: DialectDomainElement) => node },
                        propertyEntry.value)
  }

  protected def parseObjectPairProperty(id: String,
                                        propertyEntry: YMapEntry,
                                        property: PropertyMapping,
                                        node: DialectDomainElement): Unit = {
    val propertyKeyMapping   = property.mapKeyProperty().option()
    val propertyValueMapping = property.mapValueProperty().option()
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
              nestedNode.setMapKeyField(propertyKeyMapping.get, pair.key.as[YScalar].text, pair.key)
              nestedNode.setMapKeyField(propertyValueMapping.get, pair.value.as[YScalar].text, pair.value)
            } catch {
              case e: UnknownMapKeyProperty =>
                ctx.violation(DialectError, e.id, s"Cannot find mapping for key map property ${e.id}", pair)
            }
            Some(nestedNode)
          } collect { case Some(elem: DialectDomainElement) => elem }
        case _ =>
          ctx.violation(
            DialectError,
            id,
            s"Cannot find mapping for property range of mapValue property: ${property.objectRange().head.value()}",
            propertyEntry
          )
          Nil
      }

      node.setObjectField(property, nested, propertyEntry.key)
    } else {
      ctx.violation(DialectError,
                    id,
                    s"Both 'mapKey' and 'mapValue' are mandatory in a map pair property mapping",
                    propertyEntry)
    }
  }

  protected def parseObjectCollectionProperty(id: String,
                                              propertyEntry: YMapEntry,
                                              property: PropertyMapping,
                                              node: DialectDomainElement,
                                              additionalProperties: Map[String, Any]): Unit = {

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
        ctx.violation(DialectError,
                      dialectDomainElement.id,
                      s"Duplicated element in collection ${dialectDomainElement.id}",
                      elementNode)
    }
  }

  protected def pathSegment(parent: String, nexts: List[String]): String = {
    var path = parent
    nexts.foreach { n =>
      path = path + "/" + n.urlComponentEncoded
    }
    path
  }

  protected def parseLiteralValue(value: YNode, property: PropertyMapping, node: DialectDomainElement): Option[_] = {

    value.tagType match {
      case YType.Bool
          if (property.literalRange().value() == (Namespace.Xsd + "boolean")
            .iri()) || property.literalRange().value() == (Namespace.Xsd + "anyType").iri() =>
        Some(value.as[Boolean])
      case YType.Bool =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    (Namespace.Xsd + "boolean").iri(),
                                                    value)
        None
      case YType.Int
          if property.literalRange().value() == (Namespace.Xsd + "integer")
            .iri() || property.literalRange().value() == (Namespace.Shapes + "number")
            .iri() || property.literalRange().value() == (Namespace.Xsd + "anyType").iri() =>
        Some(value.as[Int])
      case YType.Int =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    (Namespace.Xsd + "integer").iri(),
                                                    value)
        None
      case YType.Str
          if property.literalRange().value() == (Namespace.Xsd + "string")
            .iri() || property.literalRange().value() == (Namespace.Xsd + "anyType").iri() =>
        Some(value.as[YScalar].text)
      case YType.Str if property.literalRange().value() == (Namespace.Xsd + "anyUri").iri() =>
        Some(value.as[YScalar].text)
      case YType.Str if property.literalRange().value() == (Namespace.Shapes + "link").iri() =>
        Some(("link", value.as[YScalar].text))
      case YType.Str
          if property.literalRange().value() == (Namespace.Xsd + "time").iri() ||
            property.literalRange().value() == (Namespace.Xsd + "date").iri() ||
            property.literalRange().value() == (Namespace.Xsd + "dateTime").iri() =>
        Some(YNode(value.value, YType.Timestamp).as[SimpleDateTime])
      case YType.Str =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    (Namespace.Xsd + "string").iri(),
                                                    value)
        None
      case YType.Float
          if property.literalRange().value() == (Namespace.Xsd + "float").iri() ||
            property.literalRange().value() == (Namespace.Shapes + "number").iri() ||
            property.literalRange().value() == (Namespace.Xsd + "double").iri() ||
            property.literalRange().value() == (Namespace.Xsd + "anyType").iri() =>
        Some(value.as[Double])
      case YType.Float =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    (Namespace.Xsd + "float").iri(),
                                                    value)
        None

      case YType.Timestamp
          if property.literalRange().value() == (Namespace.Xsd + "time").iri() ||
            property.literalRange().value() == (Namespace.Xsd + "date").iri() ||
            property.literalRange().value() == (Namespace.Xsd + "dateTime").iri() ||
            property.literalRange().value() == (Namespace.Xsd + "anyType").iri() =>
        Some(value.as[SimpleDateTime])

      case YType.Timestamp if property.literalRange().value() == (Namespace.Xsd + "string").iri() =>
        Some(value.as[YScalar].text)

      case YType.Timestamp =>
        ctx.inconsistentPropertyRangeValueViolation(node.id,
                                                    property,
                                                    property.literalRange().value(),
                                                    (Namespace.Xsd + "dateTime").iri(),
                                                    value)
        None

      case _ =>
        ctx.violation(DialectError, node.id, s"Unsupported scalar type ${value.tagType}", value)
        None
    }
  }

  protected def setLiteralValue(value: YNode, property: PropertyMapping, node: DialectDomainElement) = {
    parseLiteralValue(value, property, node) match {
      case Some(b: Boolean)          => node.setLiteralField(property, b, value)
      case Some(i: Int)              => node.setLiteralField(property, i, value)
      case Some(f: Float)            => node.setLiteralField(property, f, value)
      case Some(d: Double)           => node.setLiteralField(property, d, value)
      case Some(s: String)           => node.setLiteralField(property, s, value)
      case Some(("link", l: String)) => node.setLinkField(property, l, value)
      case Some(d: SimpleDateTime)   => node.setLiteralField(property, d, value)
      case _                         => // ignore
    }
  }

  protected def parseLiteralProperty(id: String,
                                     propertyEntry: YMapEntry,
                                     property: PropertyMapping,
                                     node: DialectDomainElement): Unit = {
    setLiteralValue(propertyEntry.value, property, node)
  }

  protected def parseLiteralCollectionProperty(id: String,
                                               propertyEntry: YMapEntry,
                                               property: PropertyMapping,
                                               node: DialectDomainElement): Unit = {
    propertyEntry.value.tagType match {
      case YType.Seq =>
        val values = propertyEntry.value
          .as[YSequence]
          .nodes
          .map { elemValue =>
            parseLiteralValue(elemValue, property, node)
          }
          .collect { case Some(v) => v }
        if (values.nonEmpty) {
          values.head match {
            case ("link", _: String) =>
              val links = values.collect { case (_, link) => link }.asInstanceOf[Seq[String]]
              node.setLinkField(property, links, propertyEntry.value)
            case _ =>
              node.setLiteralField(property, values, propertyEntry.value)
          }
        } else {
          node.setLiteralField(property, values, propertyEntry.value)
        }
      case _ =>
        parseLiteralValue(propertyEntry.value, property, node) match {
          case Some(("link", v)) => node.setLiteralField(property, Seq(v), propertyEntry.value)
          case Some(v)           => node.setLiteralField(property, Seq(v), propertyEntry.value)
          case _                 => // ignore
        }
    }
  }

  protected def parseNestedNode(path: String,
                                id: String,
                                entry: YNode,
                                mapping: NodeMappable,
                                additionalProperties: Map[String, Any]): Option[DialectDomainElement] =
    parseNode(path, id, entry, mapping, additionalProperties)

  protected def dispatchNodeMap(nodeMap: YMap): String =
    if (nodeMap.key("$include").isDefined)
      "$include"
    else if (nodeMap.key("$ref").isDefined)
      "$ref"
    else
      "inline"

  protected def resolveLink(ast: YNode, mapping: NodeMappable, id: String): Option[DialectDomainElement] = {
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

  protected def resolvedPath(str: String): String = {
    val base = root.location
    val fullPath =
      if (str.startsWith("/")) str
      else if (str.contains("://")) str
      else if (str.startsWith("#")) base.split("#").head + str
      else basePath(base) + str
    if (fullPath.contains("#")) {
      val parts = fullPath.split("#")
      platform.resolvePath(parts.head) + "#" + parts.last
    } else {
      platform.resolvePath(fullPath)
    }
  }

  protected def basePath(path: String): String = {
    val withoutHash = if (path.contains("#")) path.split("#").head else path
    withoutHash.splitAt(withoutHash.lastIndexOf("/"))._1 + "/"
  }

  protected def resolveJSONPointer(map: YMap, mapping: NodeMappable, id: String): Option[DialectDomainElement] = {
    val mappingIds = allNodeMappingIds(mapping)
    val entry      = map.key("$ref").get
    val pointer    = entry.value.as[String]
    val fullPointer = if (pointer.startsWith("#")) {
      root.location + pointer
    } else {
      resolvedPath(pointer)
    }

    ctx.findJsonPointer(fullPointer) map { node =>
      if (mappingIds.contains(node.definedBy.id)) {
        node
          .link(pointer, Annotations(map))
          .asInstanceOf[DialectDomainElement]
          .withId(id)
      } else {
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
                                    isRef: Boolean = false) = {
    val refTuple = ctx.link(propertyEntry.value) match {
      case Left(key) =>
        (key, ctx.declarations.findAnyDialectDomainElement(key, SearchScope.Fragments))
      case _ =>
        val text = propertyEntry.value.as[YScalar].text
        (text, ctx.declarations.findAnyDialectDomainElement(text, SearchScope.All))
    }
    refTuple match {
      case (text: String, Some(s)) =>
        AMLPlugin.registry.findNode(s.definedBy.id) match {
          case Some((dialect, _)) =>
            ctx.nestedDialects ++= Seq(dialect)
            val linkedExternal = s
              .link(text, Annotations(propertyEntry.value))
              .asInstanceOf[DialectDomainElement]
              .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            if (isRef) linkedExternal.annotations += RefInclude()
            node.setObjectField(mapping, linkedExternal, propertyEntry.value)
          case None =>
            ctx.violation(DialectError,
                          id,
                          s"Cannot find dialect for anyNode node mapping ${s.definedBy.id}",
                          propertyEntry.value)
        }
      case _ =>
        ctx.violation(
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
                                           node: DialectDomainElement) = {
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
        AMLPlugin.registry.findNode(s.definedBy.id) match {
          case Some((dialect, _)) =>
            ctx.nestedDialects ++= Seq(dialect)
            val linkedExternal = s
              .link(pointer, Annotations(map))
              .asInstanceOf[DialectDomainElement]
              .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            node.setObjectField(mapping, linkedExternal, map)
          case None =>
            ctx.violation(DialectError, id, s"Cannot find dialect for anyNode node mapping ${s.definedBy.id}", map)
        }
      case None =>
        ctx.violation(
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
                               additionalProperties: Map[String, Any],
                               rootNode: Boolean): String = {
    if (rootNode && ctx.dialect.documents().selfEncoded().option().getOrElse(false)) {
      defaultId // if this is self-encoded just reuse the dialectId computed and don't try to generate a different identifier
    } else {
      if (nodeMap.key("$id").isDefined) {
        explicitNodeId(node, nodeMap, path, defaultId, mapping)
      } else if (mapping.idTemplate.nonEmpty) {
        templateNodeId(node, nodeMap, path, defaultId, mapping)
      } else if (mapping.primaryKey().nonEmpty) {
        primaryKeyNodeId(node, nodeMap, path, defaultId, mapping, additionalProperties)
      } else {
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
    } else {
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
      var variable = varMatch.replace("{", "").replace("}", "")
      nodeMap.key(variable) match {
        case Some(entry) =>
          val value = entry.value.value.toString
          template = template.replace(varMatch, value)
        case None =>
          ctx.violation(DialectError, node.id, s"Missing ID template variable '$variable' in node", nodeMap)
      }
    }
    if (template.contains("://"))
      template
    else
      root.location + template
  }

  protected def primaryKeyNodeId(node: DialectDomainElement,
                                 nodeMap: YMap,
                                 path: Seq[String],
                                 defaultId: String,
                                 mapping: NodeMapping,
                                 additionalProperties: Map[String, Any]): String = {
    var allFound           = true
    var keyId: Seq[String] = Seq()
    mapping.primaryKey().foreach { key =>
      val propertyName = key.name().value()
      nodeMap.entries.find(_.key.as[YScalar].text == propertyName) match {
        case Some(entry) if scalarYType(entry) =>
          keyId = keyId ++ Seq(entry.value.toString())
        case _ =>
          additionalProperties.get(key.nodePropertyMapping().value()) match {
            case Some(v) => keyId = keyId ++ Seq(v.toString)
            case _ =>
              ctx.violation(DialectError, node.id, s"Cannot find unique mandatory property '$propertyName'", nodeMap)
              allFound = false
          }
      }
    }
    if (allFound) { path.map(_.urlEncoded).mkString("/") + "/" + keyId.mkString("_").urlEncoded } else { defaultId }
  }

  private def checkTarget(patch: DialectInstancePatch): DialectInstancePatch = {
    map.key("$target") match {
      case Some(entry) if entry.value.tagType == YType.Str =>
        patch.withExtendsModel(platform.resolvePath(entry.value.as[String]))

      case Some(entry) =>
        ctx.violation(DialectError, patch.id, "Patch $target must be a valid URL", entry.value)

      case _ => // ignore
    }
    patch
  }

}
