package amf.plugins.document.vocabularies.parser.dialects

import amf.core.Root
import amf.core.annotations.Aliases
import amf.core.metamodel.document.FragmentModel
import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.model.domain.{AmfScalar, DomainElement}
import amf.core.parser.SearchScope.All
import amf.core.parser.{Annotations, BaseSpecParser, ErrorHandler, FutureDeclarations, ParserContext, _}
import amf.core.utils._
import amf.core.vocabulary.Namespace
import amf.plugins.document.vocabularies.metamodel.document.DialectModel
import amf.plugins.document.vocabularies.metamodel.domain.{DocumentsModelModel, NodeMappingModel, PropertyMappingModel}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectFragment, DialectLibrary, Vocabulary}
import amf.plugins.document.vocabularies.model.domain._
import amf.plugins.document.vocabularies.parser.common.{AnnotationsParser, SyntaxErrorReporter}
import amf.plugins.document.vocabularies.parser.vocabularies.VocabularyDeclarations
import amf.plugins.features.validation.ParserSideValidations.DialectError
import org.yaml.model._

import scala.collection.mutable

class DialectDeclarations(var nodeMappings: Map[String, NodeMappable] = Map(),
                          errorHandler: Option[ErrorHandler],
                          futureDeclarations: FutureDeclarations)
    extends VocabularyDeclarations(Map(), Map(), Map(), Map(), Map(), errorHandler, futureDeclarations) {

  /** Get or create specified library. */
  override def getOrCreateLibrary(alias: String): DialectDeclarations = {
    libraries.get(alias) match {
      case Some(lib: DialectDeclarations) => lib
      case _ =>
        val result =
          new DialectDeclarations(errorHandler = errorHandler, futureDeclarations = EmptyFutureDeclarations())
        libraries = libraries + (alias -> result)
        result
    }
  }

  def +=(nodeMapping: NodeMappable): DialectDeclarations = {
    nodeMappings += (nodeMapping.name.value() -> nodeMapping)
    this
  }

  def registerNodeMapping(nodeMapping: NodeMappable): DialectDeclarations = {
    nodeMappings += (nodeMapping.name.value() -> nodeMapping)
    this
  }

  def findNodeMapping(key: String, scope: SearchScope.Scope): Option[NodeMappable] =
    findForType(key, _.asInstanceOf[DialectDeclarations].nodeMappings, scope) collect {
      case nm: NodeMappable => nm
    }

  def findClassTerm(key: String, scope: SearchScope.Scope): Option[ClassTerm] =
    findForType(key, _.asInstanceOf[DialectDeclarations].classTerms, scope) match {
      case Some(ct: ClassTerm) => Some(ct)
      case _                   => resolveExternal(key).map(ClassTerm().withId(_))
    }

  def findPropertyTerm(key: String, scope: SearchScope.Scope): Option[PropertyTerm] =
    findForType(key, _.asInstanceOf[DialectDeclarations].propertyTerms, scope) match {
      case Some(pt: PropertyTerm) => Some(pt)
      case _                      => resolveExternal(key).map(DatatypePropertyTerm().withId(_))
    }

  override def declarables(): Seq[DomainElement] = nodeMappings.values.toSeq

}

trait DialectSyntax { this: DialectContext =>
  val dialect: Map[String, Boolean] = Map(
    "$dialect"     -> false,
    "dialect"      -> true,
    "version"      -> true,
    "usage"        -> false,
    "external"     -> false,
    "uses"         -> false,
    "nodeMappings" -> false,
    "documents"    -> false
  )

  val library: Map[String, Boolean] = Map(
    "usage"        -> false,
    "external"     -> false,
    "uses"         -> false,
    "nodeMappings" -> false
  )

  val nodeMapping: Map[String, Boolean] = Map(
    "classTerm"  -> true,
    "mapping"    -> false,
    "idProperty" -> false,
    "idTemplate" -> false,
    "patch"      -> false
  )

  val fragment: Map[String, Boolean] = Map(
    "usage"    -> false,
    "external" -> false,
    "uses"     -> false
  ) ++ nodeMapping

  val propertyMapping: Map[String, Boolean] = Map(
    "propertyTerm"          -> true,
    "range"                 -> false,
    "mapKey"                -> false,
    "mapValue"              -> false,
    "mandatory"             -> false,
    "pattern"               -> false,
    "sorted"                -> false,
    "minimum"               -> false,
    "maximum"               -> false,
    "allowMultiple"         -> false,
    "enum"                  -> false,
    "typeDiscriminatorName" -> false,
    "typeDiscriminator"     -> false,
    "unique"                -> false,
    "patch"                 -> false
  )

  val documentsMapping: Map[String, Boolean] = Map(
    "root"      -> false,
    "fragments" -> false,
    "library"   -> false,
    "options"   -> false
  )

  val documentsMappingOptions: Map[String, Boolean] = Map(
    "selfEncoded"      -> false,
    "declarationsPath" -> false
  )

  def closedNode(nodeType: String, id: String, map: YMap): Unit = {
    val allowedProps = nodeType match {
      case "dialect"                 => dialect
      case "library"                 => library
      case "fragment"                => fragment
      case "nodeMapping"             => nodeMapping
      case "propertyMapping"         => propertyMapping
      case "documentsMapping"        => documentsMapping
      case "documentsMappingOptions" => documentsMappingOptions
    }
    map.map.keySet.map(_.as[YScalar].text).foreach { property =>
      if (!isAnnotation(property)) {
        allowedProps.get(property) match {
          case Some(_) => // correct
          case None    => closedNodeViolation(id, property, nodeType, map)
        }
      }
    }

    allowedProps.foreach {
      case (propName, mandatory) =>
        val props = map.map.keySet.map(_.as[YScalar].text)
        if (mandatory) {
          if (!props.contains(propName)) {
            missingPropertyViolation(id, propName, nodeType, map)
          }
        }
    }
  }

  private def isAnnotation(property: String): Boolean =
    (property.startsWith("(") && property.endsWith(")")) || property.startsWith("x-")

  def link(node: YNode): Either[String, YNode] = {
    node match {
      case _ if isInclude(node) => Left(node.as[YScalar].text)
      case _                    => Right(node)
    }
  }

  private def isInclude(node: YNode) = node.tagType == YType.Include
}

class DialectContext(private val wrapped: ParserContext, private val ds: Option[DialectDeclarations] = None)
    extends ParserContext(wrapped.rootContextDocument, wrapped.refs, wrapped.futureDeclarations, wrapped.parserCount)
    with DialectSyntax
    with SyntaxErrorReporter {

  val declarations: DialectDeclarations =
    ds.getOrElse(new DialectDeclarations(errorHandler = Some(this), futureDeclarations = futureDeclarations))

}

case class ReferenceDeclarations(references: mutable.Map[String, Any] = mutable.Map())(implicit ctx: DialectContext) {
  def +=(alias: String, unit: BaseUnit): Unit = {
    references += (alias -> unit)
    unit match {
      case d: Vocabulary =>
        ctx.declarations
          .registerUsedVocabulary(alias, d) // to keep track of the uses: alias -> vocab, useful for annotations
        val library = ctx.declarations.getOrCreateLibrary(alias)
        d.declares.foreach {
          case prop: PropertyTerm => library.registerTerm(prop)
          case cls: ClassTerm     => library.registerTerm(cls)
        }
      case m: DeclaresModel =>
        val library = ctx.declarations.getOrCreateLibrary(alias)
        m.declares.foreach {
          case nodeMapping: NodeMappable => library.registerNodeMapping(nodeMapping)
          case decl                      => library += decl
        }
      case f: DialectFragment =>
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

case class DialectsReferencesParser(dialect: Dialect, map: YMap, references: Seq[ParsedReference])(
    implicit ctx: DialectContext) {

  def parse(location: String): ReferenceDeclarations = {
    val result = ReferenceDeclarations()
    parseLibraries(dialect, result, location)
    parseExternals(result, location)

    references.foreach {
      case ParsedReference(f: DialectFragment, origin: Reference, None) => result += (origin.url, f)
      case _                                                            =>
    }

    result
  }

  private def parseLibraries(dialect: Dialect, result: ReferenceDeclarations, id: String): Unit = {
    map.key(
      "uses",
      entry =>
        entry.value
          .as[YMap]
          .entries
          .foreach(e => {
            val alias: String = e.key.as[YScalar].text
            val url: String   = library(e)
            target(url).foreach {
              case module: Vocabulary =>
                collectAlias(dialect, alias -> (module.base.value(), url))
                result += (alias, module)
              case module: DeclaresModel =>
                collectAlias(dialect, alias -> (module.id, url))
                result += (alias, module)
              case other =>
                ctx.violation(DialectError, id, s"Expected vocabulary module but found: $other", e) // todo Uses should only reference modules...
            }
          })
    )
  }

  private def target(url: String): Option[BaseUnit] =
    references.find(r => r.origin.url.equals(url)).map(_.unit)

  private def library(e: YMapEntry): String = e.value.tagType match {
    case YType.Include => e.value.as[YScalar].text
    case _             => e.value
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

  private def parseExternals(result: ReferenceDeclarations, id: String): Unit = {
    map.key(
      "external",
      entry =>
        entry.value
          .as[YMap]
          .entries
          .foreach(e => {
            val alias: String = e.key.as[YScalar].text
            val base: String  = e.value
            val external      = External()
            result += external.withAlias(alias).withBase(base)
          })
    )
  }
}

class DialectsParser(root: Root)(implicit override val ctx: DialectContext)
    extends BaseSpecParser
    with AnnotationsParser {

  val map: YMap        = root.parsed.asInstanceOf[SyamlParsedDocument].document.as[YMap]
  val dialect: Dialect = Dialect(Annotations(map)).withLocation(root.location).withId(root.location)

  def parseDocument(): BaseUnit = {
    map.key("dialect", entry => {
      val value = ValueNode(entry.value)
      dialect.set(DialectModel.Name, value.string(), Annotations(entry))
    })

    map.key("usage", entry => {
      val value = ValueNode(entry.value)
      dialect.set(DialectModel.Usage, value.string(), Annotations(entry))
    })

    map.key(
      "version",
      entry => {
        val value   = ValueNode(entry.value)
        val version = value.text().value.toString
        dialect.set(DialectModel.Version, AmfScalar(version, Annotations(entry.value)), Annotations(entry))
      }
    )

    // closed node validation
    ctx.closedNode("dialect", dialect.id, map)

    val references =
      DialectsReferencesParser(dialect, map, root.references).parse(dialect.location().getOrElse(dialect.id))

    if (ctx.declarations.externals.nonEmpty)
      dialect.withExternals(ctx.declarations.externals.values.toSeq)

    parseDeclarations(root, map)

    val declarables = ctx.declarations.declarables()
    declarables.foreach {
      case unionNodeMapping: UnionNodeMapping =>
        checkNodeMappableReferences(unionNodeMapping)

      case nodeMapping: NodeMapping =>
        nodeMapping.propertiesMapping().foreach { propertyMapping =>
          checkNodeMappableReferences(propertyMapping)
        }
    }

    if (declarables.nonEmpty) dialect.withDeclares(declarables)
    if (references.baseUnitReferences().nonEmpty) dialect.withReferences(references.baseUnitReferences())

    parseDocumentsMapping(map, dialect.id)

    // resolve unresolved references
    dialect.declares.foreach {
      case dec: NodeMapping =>
        if (!dec.isUnresolved) {
          ctx.futureDeclarations.resolveRef(dec.name.value(), dec)
        }
      case _ => //
    }
    ctx.futureDeclarations.resolve()

    dialect
  }

  protected def parseDeclarations(root: Root, map: YMap): Unit = {
    val parent = root.location + "#/declarations"
    parseNodeMappingDeclarations(map, parent)
  }

  /**
    * Transforming URIs in references of node ranges and discrminators into actual
    * label with a reference
    */
  protected def checkNodeMappableReferences[T <: DomainElement](mappable: NodeWithDiscriminator[T]) = {
    val mapped = mappable.objectRange().map { nodeMappingRef =>
      if (nodeMappingRef.value() == (Namespace.Meta + "anyNode").iri()) {
        Some(nodeMappingRef.value())
      } else {
        ctx.declarations.findNodeMapping(nodeMappingRef.value(), All) match {
          case Some(mapping) => Some(mapping.id)
          case _ =>
            ctx.missingPropertyRangeViolation(
              nodeMappingRef.value(),
              mappable.id,
              mappable.fields
                .entry(PropertyMappingModel.ObjectRange)
                .map(_.value.annotations)
                .getOrElse(mappable.annotations)
            )
            None
        }
      }
    }
    val refs = mapped.collect { case Some(ref) => ref }
    if (refs.nonEmpty) mappable.withObjectRange(refs)

    // Setting ids we left unresolved in typeDiscriminators
    Option(mappable.typeDiscriminator()) match {
      case Some(typeDiscriminators) =>
        val mapped = typeDiscriminators.foldLeft(Map[String, String]()) {
          case (acc, (nodeMappingRef, alias)) =>
            ctx.declarations.findNodeMapping(nodeMappingRef, All) match {
              case Some(mapping) => acc.updated(mapping.id, alias)
              case _ =>
                ctx.missingPropertyRangeViolation(
                  nodeMappingRef,
                  mappable.id,
                  mappable.fields
                    .entry(PropertyMappingModel.TypeDiscriminator)
                    .map(_.value.annotations)
                    .getOrElse(mappable.annotations)
                )
                acc
            }
        }
        mappable.withTypeDiscriminator(mapped)
      case _ => // ignore
    }
  }

  private def parseNodeMappingDeclarations(map: YMap, parent: String): Unit = {
    map.key("nodeMappings").foreach { e =>
      e.value.tagType match {
        case YType.Map =>
          e.value.as[YMap].entries.foreach { entry =>
            parseNodeMapping(entry, {
              case nodeMapping: NodeMapping      => nodeMapping.withName(entry.key).adopted(parent)
              case nodeMapping: UnionNodeMapping => nodeMapping.withName(entry.key).adopted(parent)
              case _                             =>
                ctx.violation(DialectError, parent, s"Error only valid node mapping or union mapping can be declared", entry)
                None
            }) match {
              case Some(nodeMapping: NodeMapping)      => ctx.declarations += nodeMapping
              case Some(nodeMapping: UnionNodeMapping) => ctx.declarations += nodeMapping
              case None                                => ctx.violation(DialectError, parent, s"Error parsing shape '$entry'", entry)
            }
          }
        case YType.Null =>
        case t          => ctx.violation(DialectError, parent, s"Invalid type $t for 'nodeMappings' node.", e.value)
      }
    }
  }

  protected def parseUnionNodeMapping(map: YMap, adopt: DomainElement => Any, fragment: Boolean = false): Option[UnionNodeMapping] = {
    val unionNodeMapping = UnionNodeMapping(map)

    adopt(unionNodeMapping)

    map.key("union",
      entry => {
        entry.value.tagType match {
          case YType.Seq =>
            try {
              unionNodeMapping.withObjectRange(entry.value.as[Seq[String]])
            } catch {
              case _: Exception =>
                ctx.violation(DialectError, unionNodeMapping.id, s"Union node mappings must be declared as lists of node mapping references", entry.value)
            }
          case _         =>
            ctx.violation(DialectError, unionNodeMapping.id, s"Union node mappings must be declared as lists of node mapping references", entry.value)
        }
      }
    )

    map.key(
      "typeDiscriminator",
      entry => {
        val types = entry.value.as[YMap]
        val typeMapping = types.entries.foldLeft(Map[String, String]()) {
          case (acc, e) =>
            val nodeMappingId = e.value.as[YScalar].text
            acc + (e.key.as[YScalar].text -> nodeMappingId)
        }
        unionNodeMapping.withTypeDiscriminator(typeMapping)
      }
    )

    map.key("typeDiscriminatorName", entry => {
      val name = ValueNode(entry.value).string().toString
      unionNodeMapping.withTypeDiscriminatorName(name)
    })


    Some(unionNodeMapping)
  }

  def parseSingleNodeMapping(map: YMap, adopt: DomainElement => Any, fragment: Boolean = false): Option[NodeMapping] = {
    val nodeMapping = NodeMapping(map)

    adopt(nodeMapping)

    if (!fragment)
      ctx.closedNode("nodeMapping", nodeMapping.id, map)

    map.key(
      "classTerm",
      entry => {
        val value       = ValueNode(entry.value)
        val classTermId = value.string().toString
        ctx.declarations.findClassTerm(classTermId, SearchScope.All) match {
          case Some(classTerm) =>
            nodeMapping.withNodeTypeMapping(classTerm.id)
          case _ =>
            ctx.violation(DialectError, nodeMapping.id, s"Cannot find class term with alias $classTermId", entry.value)
        }
      }
    )

    map.key(
      "patch",
      entry => {
        val patchMethod = ValueNode(entry.value).string().toString
        if (NodeMappingModel.ALLOWED_MERGE_POLICY.contains(patchMethod)) {
          nodeMapping.withMergePolicy(patchMethod)
        } else {
          ctx.violation(DialectError,
            nodeMapping.id,
            s"Unsupported node mapping patch operation '$patchMethod'",
            entry.value)
        }
      }
    )

    map.key(
      "mapping",
      entry => {
        val properties = entry.value.as[YMap].entries.map { entry =>
          parsePropertyMapping(
            entry,
            propertyMapping =>
              propertyMapping
                .withName(entry.key)
                .adopted(nodeMapping.id + "/property/" + entry.key.as[YScalar].text.urlComponentEncoded))
        }
        nodeMapping.withPropertiesMapping(properties)
      }
    )

    map.key(
      "idTemplate",
      entry => {
        val template = entry.value.as[String]
        validateTemplate(template, map, nodeMapping.propertiesMapping())
        nodeMapping.withIdTemplate(template)
      }
    )

    parseAnnotations(map, nodeMapping, ctx.declarations)

    Some(nodeMapping)
  }

  def parseNodeMapping(entry: YMapEntry, adopt: DomainElement => Any, fragment: Boolean = false): Option[NodeMappable] = {
    entry.value.tagType match {
        // 1) inlined node mapping
      case YType.Map =>
        val map         = entry.value.as[YMap]
        if (map.key("union").isDefined) {
          parseUnionNodeMapping(map, adopt, fragment)
        } else {
          parseSingleNodeMapping(map, adopt, fragment)
        }

        // 2) reference linking a declared node mapping
      case YType.Str if entry.value.toOption[YScalar].isDefined =>
        val refTuple = ctx.link(entry.value) match {
          case Left(key) =>
            (key, ctx.declarations.findNodeMapping(key, SearchScope.Fragments))
          case _ =>
            val text = entry.value.as[YScalar].text
            (text, ctx.declarations.findNodeMapping(text, SearchScope.Named))
        }
        refTuple match {
          case (text: String, Some(s)) =>
            val linkedNode = s
              .link(text, Annotations(entry.value))
              .asInstanceOf[NodeMapping]
              .withName(text) // we setup the local reference in the name
            adopt(linkedNode) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            Some(linkedNode)
          case (text: String, _) =>
            val linkedNode = NodeMapping(map)
            adopt(linkedNode)
            linkedNode.unresolved(text, map)
            Some(linkedNode)
        }

        // 3) node mapping included from a fragment
      case YType.Include if entry.value.toOption[YScalar].isDefined =>
        val refTuple = ctx.link(entry.value) match {
          case Left(key) =>
            (key, ctx.declarations.findNodeMapping(key, SearchScope.Fragments))
          case _ =>
            val text = entry.value.as[YScalar].text
            (text, ctx.declarations.findNodeMapping(text, SearchScope.Named))
        }
        refTuple match {
          case (text: String, Some(s)) =>
            val linkedNode = s
              .link(text, Annotations(entry.value))
              .asInstanceOf[NodeMapping]
              .withName(text) // we setup the local reference in the name
            adopt(linkedNode) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            Some(linkedNode)
          case (text: String, _) =>
            val nodeMappingTmp = adopt(NodeMapping()).asInstanceOf[NodeMapping]
            val suffix         = nodeMappingTmp.id.split("#").head
            ctx.missingFragmentViolation(text, nodeMappingTmp.id.replace(suffix, ""), entry.value)
            None
        }

      case _ => None
    }
  }

  def parsePropertyMapping(entry: YMapEntry, adopt: PropertyMapping => Any): PropertyMapping = {
    val map             = entry.value.as[YMap]
    val propertyMapping = PropertyMapping(map)

    adopt(propertyMapping)
    ctx.closedNode("propertyMapping", propertyMapping.id, map)

    map.key(
      "propertyTerm",
      entry => {
        val value          = ValueNode(entry.value)
        val propertyTermId = value.string().toString
        ctx.declarations.findPropertyTerm(propertyTermId, SearchScope.All) match {
          case Some(propertyTerm) =>
            propertyMapping.withNodePropertyMapping(propertyTerm.id)
          case _ =>
            ctx.violation(DialectError,
                          propertyMapping.id,
                          s"Cannot find property term with alias $propertyTermId",
                          entry.value)
        }
      }
    )

    map.key(
      "range",
      entry => {
        entry.value.tagType match {
          case YType.Seq =>
            propertyMapping.withObjectRange(entry.value.as[Seq[String]])
          case _ =>
            val value = ValueNode(entry.value)
            val range = value.string().toString
            range match {
              case "string" | "integer" | "boolean" | "float" | "decimal" | "double" | "duration" | "dateTime" |
                  "time" | "date" | "anyUri" | "anyType" =>
                propertyMapping.withLiteralRange((Namespace.Xsd + range).iri())
              case "link"    => propertyMapping.withLiteralRange((Namespace.Shapes + "link").iri())
              case "number"  => propertyMapping.withLiteralRange((Namespace.Shapes + "number").iri())
              case "uri"     => propertyMapping.withLiteralRange((Namespace.Xsd + "anyUri").iri())
              case "any"     => propertyMapping.withLiteralRange((Namespace.Xsd + "anyType").iri())
              case "anyNode" => propertyMapping.withObjectRange(Seq((Namespace.Meta + "anyNode").iri()))
              case nodeMappingId =>
                propertyMapping
                  .withObjectRange(Seq(nodeMappingId)) // temporary until we can resolve all nodeMappings after finishing parsing declarations
            }
        }
      }
    )

    map.key(
      "mapKey",
      entry => {
        val propertyTermId = ValueNode(entry.value).string().toString
        ctx.declarations.findPropertyTerm(propertyTermId, All) match {
          case Some(term) => propertyMapping.withMapKeyProperty(term.id)
          case _ =>
            ctx.violation(DialectError,
                          propertyMapping.id,
                          s"Cannot find property term with alias $propertyTermId",
                          entry.value)
        }
      }
    )

    map.key(
      "patch",
      entry => {
        val patchMethod = ValueNode(entry.value).string().toString
        if (PropertyMappingModel.ALLOWED_MERGE_POLICY.contains(patchMethod)) {
          propertyMapping.withMergePolicy(patchMethod)
        } else {
          ctx.violation(DialectError,
                        propertyMapping.id,
                        s"Unsupported property mapping patch operation '$patchMethod'",
                        entry.value)
        }
      }
    )

    map.key(
      "mapValue",
      entry => {
        val propertyTermId = ValueNode(entry.value).string().toString
        ctx.declarations.findPropertyTerm(propertyTermId, All) match {
          case Some(term) => propertyMapping.withMapValueProperty(term.id)
          case _ =>
            ctx.violation(DialectError,
                          propertyMapping.id,
                          s"Cannot find property term with alias $propertyTermId",
                          entry.value)
        }
      }
    )

    map.key("mandatory", entry => {
      val required = ValueNode(entry.value).boolean().toBool
      val value    = if (required) 1 else 0
      propertyMapping.withMinCount(value)
    })

    map.key("pattern", entry => {
      val value = ValueNode(entry.value).string().toString
      propertyMapping.withPattern(value)
    })

    map.key(
      "minimum",
      entry => {
        entry.value.tagType match {
          case YType.Int =>
            val value = ValueNode(entry.value).integer().value
            propertyMapping.withMinimum(value.asInstanceOf[Int].toDouble)
          case _ =>
            val value = ValueNode(entry.value).float().value
            propertyMapping.withMinimum(value.asInstanceOf[Double])
        }
      }
    )

    map.key(
      "unique",
      entry => {
        entry.value.tagType match {
          case YType.Bool =>
            val booleanValue = ValueNode(entry.value).boolean().toBool
            propertyMapping.withUnique(booleanValue)
          case _ =>
            ctx.violation(DialectError, "Unique property in a property mapping must be a boolean value", entry.value)
        }
      }
    )

    map.key(
      "maximum",
      entry => {
        entry.value.tagType match {
          case YType.Int =>
            val value = ValueNode(entry.value).integer().value
            propertyMapping.withMaximum(value.asInstanceOf[Int].toFloat)
          case _ =>
            val value = ValueNode(entry.value).float().value
            propertyMapping.withMaximum(value.asInstanceOf[Double])
        }
      }
    )

    map.key("allowMultiple", entry => {
      val value = ValueNode(entry.value).boolean().toBool
      propertyMapping.withAllowMultiple(value)
    })

    map.key("sorted", entry => {
      val sorted = ValueNode(entry.value).boolean().toBool
      propertyMapping.withSorted(sorted)
    })

    map.key(
      "enum",
      entry => {
        val values = entry.value.as[YSequence].nodes.map { node =>
          node.value match {
            case scalar: YScalar => Some(scalar.value)
            case _ =>
              ctx.violation(DialectError, "Cannot create enumeration constraint from not scalar value", node)
              None
          }
        }
        propertyMapping.withEnum(values.collect { case Some(v) => v })
      }
    )

    map.key(
      "typeDiscriminator",
      entry => {
        val types = entry.value.as[YMap]
        val typeMapping = types.entries.foldLeft(Map[String, String]()) {
          case (acc, e) =>
            val nodeMappingId = e.value.as[YScalar].text
            acc + (e.key.as[YScalar].text -> nodeMappingId)
        }
        propertyMapping.withTypeDiscriminator(typeMapping)
      }
    )

    map.key("typeDiscriminatorName", entry => {
      val name = ValueNode(entry.value).string().toString
      propertyMapping.withTypeDiscriminatorName(name)
    })

    // TODO: check dependencies among properties

    parseAnnotations(map, propertyMapping, ctx.declarations)

    propertyMapping
  }

  def validateTemplate(template: String, map: YMap, propMappings: Seq[PropertyMapping]): Unit = {
    val regex = "(\\{[^}]+\\})".r
    regex.findAllIn(template).foreach { varMatch =>
      val variable = varMatch.replace("{", "").replace("}", "")
      propMappings.find(_.name().value() == variable) match {
        case Some(prop) =>
          if (prop.minCount().option().getOrElse(0) != 1)
            ctx.violation(DialectError,
                          prop.id,
                          s"PropertyMapping for idTemplate variable '$variable' must be mandatory",
                          map)
        case None =>
          ctx.violation(DialectError, "", s"Missing propertyMapping for idTemplate variable '$variable'", map)
      }
    }
  }

  private def parseDocumentsMapping(map: YMap, parent: String): Unit = {
    val documentsMapping = DocumentsModel().withId(parent + "#/documents")
    map.key("documents").foreach { e =>
      ctx.closedNode("documentsMapping", documentsMapping.id, e.value.as[YMap])
      parseRootDocumentMapping(e.value, documentsMapping.id) foreach { rootMapping: DocumentMapping =>
        documentsMapping.withRoot(rootMapping)
      }
      parseFragmentsMapping(e.value, documentsMapping.id) map { fragmentMappings: Seq[DocumentMapping] =>
        documentsMapping.withFragments(fragmentMappings)
      }
      parseLibraries(e.value, documentsMapping.id).foreach(documentsMapping.withLibrary)
      parseDocumentsOptions(e.value, documentsMapping)
    }

    dialect.withDocuments(documentsMapping)
  }

  def parseRootDocumentMapping(value: YNode, parent: String): Option[DocumentMapping] = {
    value.as[YMap].key("root") match {
      case Some(entry: YMapEntry) if entry.value.tagType == YType.Map =>
        val name             = s"${dialect.name().value()} ${dialect.version().value()}"
        val rootMap          = entry.value.as[YMap]
        val documentsMapping = DocumentMapping(map).withDocumentName(name).withId(parent + "/root")
        rootMap.key(
          "encodes",
          entry => {
            val nodeId = entry.value.as[YScalar].text
            ctx.declarations.findNodeMapping(nodeId, SearchScope.All) match {
              case Some(nodeMapping) => Some(documentsMapping.withEncoded(nodeMapping.id))
              case _                 => None // TODO: violation here
            }
          }
        )
        rootMap.key(
          "declares",
          entry => {
            val declaresMap = entry.value.as[YMap]
            val declarations: Seq[Option[PublicNodeMapping]] = declaresMap.entries.map {
              declarationEntry =>
                val declarationId   = declarationEntry.value.as[YScalar].text
                val declarationName = declarationEntry.key.as[YScalar].text
                val declarationMapping = PublicNodeMapping(declarationEntry)
                  .withName(declarationName)
                  .withId(parent + "/declaration/" + declarationName.urlComponentEncoded)
                ctx.declarations.findNodeMapping(declarationId, SearchScope.All) match {
                  case Some(nodeMapping) => Some(declarationMapping.withMappedNode(nodeMapping.id))
                  case _                 => None // TODO: violation here
                }
            }
            documentsMapping.withDeclaredNodes(declarations.collect { case m: Some[PublicNodeMapping] => m.get })
          }
        )
        Some(documentsMapping)
      case _ => None
    }
  }

  def parseFragmentsMapping(value: YNode, parent: String): Option[Seq[DocumentMapping]] = {
    value.as[YMap].key("fragments") match {
      case Some(entry: YMapEntry) =>
        entry.value.as[YMap].key("encodes") match {
          case Some(entry: YMapEntry) =>
            val docs = entry.value.as[YMap].entries.map { fragmentEntry =>
              val fragmentName = fragmentEntry.key.as[YScalar].text
              val nodeId       = fragmentEntry.value.as[YScalar].text
              val documentsMapping = DocumentMapping(fragmentEntry.value)
                .withDocumentName(fragmentName)
                .withId(parent + s"/fragments/${fragmentName.urlComponentEncoded}")
              ctx.declarations.findNodeMapping(nodeId, SearchScope.All) match {
                case Some(nodeMapping) => Some(documentsMapping.withEncoded(nodeMapping.id))
                case _ =>
                  ctx.missingTermViolation(nodeId, parent, fragmentEntry)
                  None
              }
            }
            Some(docs.filter(_.isDefined).map(_.get).asInstanceOf[Seq[DocumentMapping]])
          case _ => None
        }
      case _ => None
    }
  }

  def parseLibraries(value: YNode, parent: String): Option[DocumentMapping] = {
    value.as[YMap].key("library") match {
      case Some(entry: YMapEntry) =>
        val name             = s"${dialect.name().value()} ${dialect.version().value()} / Library"
        val documentsMapping = DocumentMapping(map).withDocumentName(name).withId(parent + "/modules")
        entry.value.as[YMap].key("declares") match {
          case Some(libraryEntry) =>
            val declaresMap = libraryEntry.value.as[YMap]
            val declarations: Seq[Option[PublicNodeMapping]] = declaresMap.entries.map { declarationEntry =>
              val declarationId   = declarationEntry.value.as[YScalar].text
              val declarationName = declarationEntry.key.as[YScalar].text
              val declarationMapping = PublicNodeMapping(declarationEntry)
                .withName(declarationName)
                .withId(parent + "/modules/" + declarationName.urlComponentEncoded)
              ctx.declarations.findNodeMapping(declarationId, SearchScope.All) match {
                case Some(nodeMapping) => Some(declarationMapping.withMappedNode(nodeMapping.id))
                case _ =>
                  ctx.missingTermViolation(declarationId, parent, libraryEntry)
                  None
              }
            }
            Some(documentsMapping.withDeclaredNodes(declarations.collect { case m: Some[PublicNodeMapping] => m.get }))
          case _ => None
        }

      case _ => None
    }
  }

  def parseDocumentsOptions(value: YNode, documentsMapping: DocumentsModel) = {
    value.as[YMap].key("options") match {
      case Some(entry: YMapEntry) =>
        entry.value.toOption[YMap] match {
          case Some(optionsMap) =>
            ctx.closedNode("documentsMappingOptions", documentsMapping.id, optionsMap)
            parseOptions(optionsMap, documentsMapping)
          case _ =>
            ctx.violation(DialectError,
                          documentsMapping.id,
                          "Options for a documents mapping must be a map",
                          entry.value)
        }
      case _ => // ignore
    }
  }

  def parseOptions(map: YMap, documentsModel: DocumentsModel): Option[Any] = {
    map.key("selfEncoded").map(v => parseSelfEncoded(v, documentsModel))
    map.key("declarationsPath").map(v => parseDeclarationsPath(v, documentsModel))
  }

  private def parseSelfEncoded(entry: YMapEntry, documentsModel: DocumentsModel) = {
    entry.value.tagType match {
      case YType.Bool =>
        val selfEncoded = ValueNode(entry.value).boolean()
        documentsModel.set(DocumentsModelModel.SelfEncoded, selfEncoded, Annotations(entry))
      case _ =>
        ctx.violation(DialectError,
                      documentsModel.id,
                      "'selfEncoded' Option for a documents mapping must be a boolean",
                      entry)
    }
  }

  private def parseDeclarationsPath(entry: YMapEntry, documentsModel: DocumentsModel) = {
    entry.value.tagType match {
      case YType.Str =>
        documentsModel.set(DocumentsModelModel.DeclarationsPath, ValueNode(entry.value).string(), Annotations(entry))
      case _ =>
        ctx.violation(DialectError,
                      documentsModel.id,
                      "'declarationsPath' Option for a documents mapping must be a String",
                      entry)
    }
  }

  def parseLibrary(): BaseUnit = {
    map.key("usage", entry => {
      val value = ValueNode(entry.value)
      dialect.set(DialectModel.Usage, value.string(), Annotations(entry))
    })

    // closed node validation
    ctx.closedNode("library", dialect.id, map)

    val references =
      DialectsReferencesParser(dialect, map, root.references).parse(dialect.location().getOrElse(dialect.id))

    if (ctx.declarations.externals.nonEmpty)
      dialect.withExternals(ctx.declarations.externals.values.toSeq)

    parseDeclarations(root, map)

    val declarables = ctx.declarations.declarables()
    declarables.foreach {
      case unionNodeMapping: UnionNodeMapping =>
        checkNodeMappableReferences(unionNodeMapping)

      case nodeMapping: NodeMapping =>
        nodeMapping.propertiesMapping().foreach { propertyMapping =>
          checkNodeMappableReferences(propertyMapping)
        }
    }
    if (declarables.nonEmpty) dialect.withDeclares(declarables)
    if (references.baseUnitReferences().nonEmpty) dialect.withReferences(references.baseUnitReferences())

    // resolve unresolved references
    dialect.declares.foreach {
      case dec: NodeMapping =>
        if (!dec.isUnresolved) {
          ctx.futureDeclarations.resolveRef(dec.name.value(), dec)
        }
      case _ => //
    }
    ctx.futureDeclarations.resolve()

    toLibrary(dialect)
  }

  protected def toLibrary(dialect: Dialect): DialectLibrary = {
    val library = DialectLibrary(dialect.annotations)
      .withId(dialect.id)
      .withLocation(dialect.location().getOrElse(dialect.id))
      .withReferences(dialect.references)

    dialect.usage.option().foreach(usage => library.withUsage(usage))

    val declares = dialect.declares
    if (declares.nonEmpty) library.withDeclares(declares)

    val externals = dialect.externals
    if (externals.nonEmpty) library.withExternals(externals)

    library
  }

  def parseFragment(): BaseUnit = {

    map.key("usage", entry => {
      val value = ValueNode(entry.value)
      dialect.set(DialectModel.Usage, value.string(), Annotations(entry))
    })

    // closed node validation
    ctx.closedNode("fragment", dialect.id, map)

    val references =
      DialectsReferencesParser(dialect, map, root.references).parse(dialect.location().getOrElse(dialect.id))

    if (ctx.declarations.externals.nonEmpty)
      dialect.withExternals(ctx.declarations.externals.values.toSeq)

    parseDeclarations(root, map)

    if (references.baseUnitReferences().nonEmpty) dialect.withReferences(references.baseUnitReferences())

    val fragment = toFragment(dialect)

    parseNodeMapping(YMapEntry(YNode("fragment"), map),
                     { case mapping: NodeMapping      => mapping.withId(fragment.id + "/fragment").withName("fragment")
                       case mapping: UnionNodeMapping => mapping.withId(fragment.id + "/fragment").withName("fragment")},
                     fragment = true) match {
      case Some(encoded: DomainElement) => fragment.fields.setWithoutId(FragmentModel.Encodes, encoded)
      case _                            => // ignore
    }

    fragment
  }

  protected def toFragment(dialect: Dialect): DialectFragment = {
    val fragment = DialectFragment(dialect.annotations)
      .withId(dialect.id)
      .withLocation(dialect.location().getOrElse(dialect.id))
      .withReferences(dialect.references)

    dialect.usage.option().foreach(usage => fragment.withUsage(usage))

    val externals = dialect.externals
    if (externals.nonEmpty) fragment.withExternals(dialect.externals)

    fragment
  }

}
