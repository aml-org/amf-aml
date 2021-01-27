package amf.plugins.document.vocabularies.parser.dialects

import amf.core.Root
import amf.core.annotations.{LexicalInformation, SourceAST, SourceLocation, SourceNode}
import amf.core.metamodel.document.FragmentModel
import amf.core.model.DataType
import amf.core.model.document.BaseUnit
import amf.core.model.domain.{AmfArray, AmfScalar, DomainElement}
import amf.core.parser.SearchScope.All
import amf.core.parser.{
  Annotations,
  BaseSpecParser,
  ScalarNode,
  SearchScope,
  SyamlParsedDocument,
  ValueNode,
  YNodeLikeOps
}
import amf.core.remote.Cache
import amf.core.utils._
import amf.core.vocabulary.Namespace
import amf.plugins.document.vocabularies.metamodel.document.DialectModel
import amf.plugins.document.vocabularies.metamodel.domain.{
  MergePolicies,
  NodeMappingModel,
  PropertyMappingModel,
  UnionNodeMappingModel
}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectFragment, DialectLibrary}
import amf.plugins.document.vocabularies.model.domain._
import amf.plugins.document.vocabularies.parser.common.AnnotationsParser
import amf.plugins.document.vocabularies.parser.dialects.DialectAstOps._
import amf.plugins.document.vocabularies.parser.instances.BaseDirective
import amf.validation.DialectValidations
import amf.validation.DialectValidations.{DialectError, EventualAmbiguity, UnavoidableAmbiguity, VariablesDefinedInBase}
import org.yaml.model._

import scala.collection.{immutable, mutable}

class DialectsParser(root: Root)(implicit override val ctx: DialectContext)
    extends BaseSpecParser
    with AnnotationsParser {

  val map: YMap        = root.parsed.asInstanceOf[SyamlParsedDocument].document.as[YMap]
  val dialect: Dialect = Dialect(Annotations(map)).withLocation(root.location).withId(root.location)

  def parseDocument(): BaseUnit = {

    map.parse("dialect", dialect setParsing DialectModel.Name)
    map.parse("usage", dialect setParsing DialectModel.Usage)
    map.parse("version", dialect setParsing DialectModel.Version)

    // closed node validation
    ctx.closedNode("dialect", dialect.id, map)

    val references = DialectsReferencesParser(dialect, map, root.references).parse()

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

    parseDocumentsMapping(map)

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
    * Recursively collects members from unions and nested unions
    * @param union union to extract members from
    * @param path path to show error on nested ambiguities
    * @tparam T
    * @return Map member -> path from root level union
    */
  private def flattenedMembersFrom[T <: DomainElement](
      union: NodeWithDiscriminator[T],
      path: Seq[UnionNodeMapping] = Nil): Map[NodeMapping, Seq[UnionNodeMapping]] = {
    membersFrom(union).flatMap {
      case anotherUnion: UnionNodeMapping if !path.contains(union) =>
        flattenedMembersFrom(anotherUnion, path :+ anotherUnion) // if member is another union get its members recursively
      case nodeMapping: NodeMapping => Some(nodeMapping -> path)
      case _                        => None
    }.toMap
  }

  protected def membersFrom[T <: DomainElement](union: NodeWithDiscriminator[T]): Seq[NodeMappable] = {
    union
      .objectRange()
      .toStream
      .flatMap { name =>
        ctx.declarations.findNodeMapping(name.value(), All)
      }
  }

  type Member            = NodeMapping
  type ConcatenatedNames = String

  /**
    * Ambiguity kinds:
    *   - Un-avoidable: cannot distinguish between two union members
    *   - Eventual: some times cannot distinguish between two union members, depending on the value of the dialect instance
    *
    * Conditions for un-avoidable ambiguity:
    *   - Set of property names is exactly the same between two union members
    *
    * Conditions for eventual ambiguity:
    *   - Set of MANDATORY property names is exactly the same between two union members
    *
    * @param union Union to calculate ambiguity over its members
    * @tparam T type of union: union node mapping or union property mapping
    */
  def checkAmbiguity[T <: DomainElement](union: NodeWithDiscriminator[T]): Unit = {
    val membersPathIndex = flattenedMembersFrom(union)
    val members          = membersPathIndex.keys.toSeq

    // We use the hash of the concatenated names as an approximation of the set equality function for performance gain
    val unavoidableCache: mutable.HashMap[ConcatenatedNames, Seq[Member]] = mutable.HashMap.empty
    val eventualCache: mutable.HashMap[ConcatenatedNames, Seq[Member]]    = mutable.HashMap.empty

    members.foreach { member =>
      updateAmbiguityCaches(member, unavoidableCache, eventualCache)
    }

    val buildPath = (m: Member) => {
      val path = membersPathIndex(m) :+ m
      path.map(_.name.value()).mkString("/")
    }

    unavoidableCache.foreach {
      case (_, members) if members.size > 1 =>
        val names = members.map(buildPath).sorted.mkString(", ")
        ctx.eh.violation(UnavoidableAmbiguity,
                         union.id,
                         s"Union is ambiguous. Members $names have the same set of property names",
                         union.annotations)
      case _ => // Ignore
    }

    eventualCache.foreach {
      case (_, members) if members.size > 1 =>
        val names = members.map(buildPath).sorted.mkString(", ")
        ctx.eh.warning(EventualAmbiguity,
                       union.id,
                       s"Union might be ambiguous. Members $names have the same set of mandatory property names",
                       union.annotations)
      case _ => // Ignore
    }
  }

  private def updateAmbiguityCaches[T <: DomainElement](
      member: Member,
      unavoidableCache: mutable.HashMap[ConcatenatedNames, Seq[Member]],
      eventualCache: mutable.HashMap[ConcatenatedNames, Seq[Member]]) = {

    val allProperties = member
      .propertiesMapping()
      .toStream
      .sortBy(_.name().value()) // Sort properties by name

    val mandatoryProperties = allProperties.filter(_.minCount().is(1))

    val concatenateNames = (properties: Seq[PropertyMapping]) => properties.map(_.name().value()).mkString("")

    val unavoidableCacheKey = concatenateNames(allProperties)
    val eventualCacheKey    = concatenateNames(mandatoryProperties)

    // Update caches
    unavoidableCache.put(unavoidableCacheKey, member +: unavoidableCache.getOrElse(unavoidableCacheKey, Nil))
    if (unavoidableCacheKey != eventualCacheKey) {
      // Unavoidable ambiguity is a superset of eventual ambiguity. We want the eventual ambiguity cache to contain clashes which are eventual and NOT unavoidable
      eventualCache.put(eventualCacheKey, member +: eventualCache.getOrElse(eventualCacheKey, Nil))
    }
  }

  /**
    * This method:
    *  1. replaces union & discriminator members references to other node mappings from their name to their id
    *  2. validates union & discriminator members exist
    *  3. checks for ambiguity
    */
  protected def checkNodeMappableReferences[T <: DomainElement](mappable: NodeWithDiscriminator[T]): Unit = {
    val memberStream      = mappable.objectRange().toStream
    val memberNamesStream = memberStream.map(member => member.value())
    val memberIdsStream   = memberNamesStream.flatMap(name => memberIdFromName(name, mappable))

    mappable match {
      case p: PropertyMapping if p.isUnion && p.typeDiscriminatorName().isNull =>
        checkAmbiguity(p)
      case u: UnionNodeMapping if u.typeDiscriminatorName().isNull =>
        checkAmbiguity(u)
      case _ => // Ignore
    }
    if (memberIdsStream.nonEmpty) mappable.withObjectRange(memberIdsStream)

    // Setting ids we left unresolved in typeDiscriminators
    Option(mappable.typeDiscriminator()) match {
      case Some(typeDiscriminators) =>
        val discriminatorValueMapping = typeDiscriminators.flatMap {
          case (name, discriminatorValue) =>
            ctx.declarations
              .findNodeMapping(name, All)
              .map(_.id -> discriminatorValue)
              .orElse {
                ctx.missingPropertyRangeViolation(
                    name,
                    mappable.id,
                    mappable.fields
                      .entry(PropertyMappingModel.TypeDiscriminator)
                      .map(_.value.annotations)
                      .getOrElse(mappable.annotations)
                )
                None
              }
        }
        mappable.withTypeDiscriminator(discriminatorValueMapping)
      case _ => // ignore
    }
  }

  private def memberIdFromName[T <: DomainElement](name: String, union: NodeWithDiscriminator[T]): Option[String] = {
    if (name == (Namespace.Meta + "anyNode").iri()) {
      Some(name)
    }
    else {
      ctx.declarations.findNodeMapping(name, All) match {
        case Some(mapping: NodeMapping) if union.isInstanceOf[PropertyMapping] =>
          // I want to search or generate the uri (and check that the term is the same if is already set it) right know, so I can throw a violation if some is wrong before start parsing the instance.
          // Also, If none instance will be parsed, but the dialect model is going to be serialized, I would be better has the terms already setting in the json ld. That way, the violation is collected now, and we don't need to do some particular, border case, logic in the json ld graph parser.
          updateMapLabelReferences(union.asInstanceOf[PropertyMapping], mapping) // Should remove this side effect
          Some(mapping.id)
        case Some(mapping) =>
          Some(mapping.id)
        case _ =>
          ctx.findInRecursiveUnits(name) match {
            case Some(recursiveId) =>
              Some(recursiveId)
            case _ =>
              ctx.missingPropertyRangeViolation(
                  name,
                  union.id,
                  union.fields
                    .entry(PropertyMappingModel.ObjectRange)
                    .map(_.value.annotations)
                    .getOrElse(union.annotations)
              )
              None
          }
      }
    }
  }

  def updateMapLabelReferences(propertyMapping: PropertyMapping, range: NodeMapping): Unit = {
    // todo: if mapKey is not defined but mapTermKey is, should we validate that all ranges contains one property with that term and if not, throw a violation?

    updateKeyMapReferences(propertyMapping, range)
    updateValueMapReferences(propertyMapping, range)
  }

  private def updateValueMapReferences(propertyMapping: PropertyMapping, range: NodeMapping): Unit = {
    propertyMapping.mapValueProperty().option().foreach { label =>
      range.propertiesMapping().find(_.name().value() == label) match {
        case Some(property) =>
          val term = property.nodePropertyMapping().option().getOrElse((Namespace.Data + label).iri())
          property.mapTermValueProperty().option() match {
            case Some(actualTerm) if term != actualTerm =>
              property.fields.removeField(PropertyMappingModel.MapTermValueProperty)
              propertyMapping.fields.removeField(PropertyMappingModel.MapValueProperty)
              ctx.differentTermsInMapKey(
                  propertyMapping.id,
                  PropertyMappingModel.MapValueProperty.value.iri(),
                  label,
                  propertyMapping.mapValueProperty().annotations()
              )
            case _ => propertyMapping.withMapTermValueProperty(term)
          }
        case _ =>
          propertyMapping.fields.removeField(PropertyMappingModel.MapValueProperty)
          ctx.missingPropertyKeyViolation(
              propertyMapping.id,
              PropertyMappingModel.MapValueProperty.value.iri(),
              label,
              propertyMapping.mapValueProperty().annotations()
          )
      }
    }
  }

  private def updateKeyMapReferences(propertyMapping: PropertyMapping, range: NodeMapping): Unit = {
    propertyMapping.mapKeyProperty().option().foreach { label =>
      range.propertiesMapping().find(_.name().value() == label) match {
        case Some(property) =>
          val term = property.nodePropertyMapping().option().getOrElse((Namespace.Data + label).iri())
          property.mapTermKeyProperty().option() match {
            case Some(actualTerm) if term != actualTerm =>
              propertyMapping.fields.removeField(PropertyMappingModel.MapTermKeyProperty)
              propertyMapping.fields.removeField(PropertyMappingModel.MapKeyProperty)
              ctx.differentTermsInMapKey(
                  propertyMapping.id,
                  PropertyMappingModel.MapKeyProperty.value.iri(),
                  label,
                  propertyMapping.mapKeyProperty().annotations()
              )
            case _ => propertyMapping.withMapTermKeyProperty(term)
          }
        case _ =>
          propertyMapping.fields.removeField(PropertyMappingModel.MapKeyProperty)
          ctx.missingPropertyKeyViolation(
              propertyMapping.id,
              PropertyMappingModel.MapKeyProperty.value.iri(),
              label,
              propertyMapping.mapKeyProperty().annotations()
          )
      }
    }
  }

  private def parseNodeMappingDeclarations(map: YMap, parent: String): Unit = {
    map.key("nodeMappings").foreach { e =>
      e.value.tagType match {
        case YType.Map =>
          e.value.as[YMap].entries.foreach { entry =>
            val nodeName = entry.key.toString
            if (AmlScalars.all.contains(nodeName)) {
              ctx.eh.violation(DialectError, parent, s"Error parsing node mapping: '$nodeName' is a reserved name", entry)
            }
            else {
              parseNodeMapping(
                  entry, {
                    case nodeMapping: NodeMappable =>
                      val name = ScalarNode(entry.key).string()
                      nodeMapping.set(NodeMappingModel.Name, name, Annotations(entry.key)).adopted(parent)
                      nodeMapping.annotations.reject(a =>
                        a.isInstanceOf[SourceAST] || a.isInstanceOf[LexicalInformation] || a
                          .isInstanceOf[SourceLocation] || a.isInstanceOf[SourceNode])
                      nodeMapping.annotations ++= Annotations(entry)
                    case _ =>
                      ctx.eh.violation(DialectError,
                                       parent,
                                       s"Error only valid node mapping or union mapping can be declared",
                                       entry)
                      None
                  }
              ) match {
                case Some(nodeMapping: NodeMapping) =>
                  ctx.declarations += nodeMapping
                case Some(nodeMapping: UnionNodeMapping) => ctx.declarations += nodeMapping
                case _                                   => ctx.eh.violation(DialectError, parent, s"Error parsing shape '$entry'", entry)
              }

            }
          }
        case YType.Null =>
        case t          => ctx.eh.violation(DialectError, parent, s"Invalid type $t for 'nodeMappings' node.", e.value)
      }
    }
  }

  protected def parseUnionNodeMapping(map: YMap,
                                      adopt: DomainElement => Any,
                                      fragment: Boolean = false): Option[UnionNodeMapping] = {
    val unionNodeMapping = UnionNodeMapping(map)

    adopt(unionNodeMapping)

    map.key(
        "union",
        entry => {
          entry.value.tagType match {
            case YType.Seq =>
              try {
                unionNodeMapping.withObjectRange(entry.value.as[Seq[String]])
              } catch {
                case _: Exception =>
                  ctx.eh.violation(DialectError,
                                   unionNodeMapping.id,
                                   s"Union node mappings must be declared as lists of node mapping references",
                                   entry.value)
              }
            case _ =>
              ctx.eh.violation(DialectError,
                               unionNodeMapping.id,
                               s"Union node mappings must be declared as lists of node mapping references",
                               entry.value)
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

    map.parse("typeDiscriminatorName", unionNodeMapping setParsing UnionNodeMappingModel.TypeDiscriminatorName)

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
              ctx.eh.violation(DialectError,
                               nodeMapping.id,
                               s"Cannot find class term with alias $classTermId",
                               entry.value)
          }
        }
    )

    map.key(
        "patch",
        entry => {
          val patchMethod = ScalarNode(entry.value).string()
          nodeMapping.set(NodeMappingModel.MergePolicy, patchMethod, Annotations(entry))
          val patchMethodValue = patchMethod.toString
          if (!MergePolicies.isAllowed(patchMethodValue)) {
            ctx.eh.violation(DialectError,
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
                    .adopted(nodeMapping.id + "/property/" + entry.key.as[YScalar].text.urlComponentEncoded))
          }
          val (withTerm, withourTerm) = properties.partition(_.nodePropertyMapping().option().nonEmpty)
          val filterProperties: immutable.Iterable[PropertyMapping] = withTerm
            .filter(_.nodePropertyMapping().option().nonEmpty)
            .groupBy(p => p.nodePropertyMapping().value())
            .flatMap({
              case (termKey, values) if values.length > 1 =>
                ctx.eh.violation(DialectError,
                                 values.head.id,
                                 s"Property term value must be unique in a node mapping. Term $termKey repeated",
                                 values.head.annotations)
                values.headOption
              case other => other._2.headOption
            })
          nodeMapping.setArrayWithoutId(NodeMappingModel.PropertiesMapping,
                                        withourTerm ++ filterProperties.toSeq,
                                        Annotations(entry))
        }
    )

    map.key(
        "extends",
        entry => {
          val reference = entry.value.toOption[YScalar]
          val parsed = reference match {
            case Some(_) => resolveNodeMappingLink(entry, adopt)
            case _       => None
          }
          parsed match {
            case Some(resolvedNodeMapping: NodeMapping) =>
              nodeMapping.withExtends(Seq(resolvedNodeMapping))
            case _ =>
              ctx.eh.violation(
                  DialectError,
                  nodeMapping.id,
                  s"Cannot find extended node mapping with reference '${reference.map(_.toString()).getOrElse("")}'",
                  entry.value)
          }
        }
    )

    map.parse("idTemplate", nodeMapping setParsing NodeMappingModel.IdTemplate)
    map.key(
        "idTemplate",
        entry => {
          val idTemplate = entry.value.as[String]
          val base       = BaseDirective.baseFrom(idTemplate)
          if (base.contains('{')) {
            ctx.eh.warning(VariablesDefinedInBase,
                           nodeMapping.id,
                           s"Base $base contains idTemplate variables overridable by $$base directive",
                           entry.value)
          }
        }
    )
    nodeMapping.nodetypeMapping.option().foreach(validateTemplate(_, map, nodeMapping.propertiesMapping()))

    parseAnnotations(map, nodeMapping, ctx.declarations)

    ctx.declarations.+=(nodeMapping)

    Some(nodeMapping)
  }

  def parseNodeMapping(entry: YMapEntry,
                       adopt: DomainElement => Any,
                       fragment: Boolean = false): Option[NodeMappable] = {
    entry.value.tagType match {
      // 1) inlined node mapping
      case YType.Map =>
        val map = entry.value.as[YMap]
        if (map.key("union").isDefined) {
          parseUnionNodeMapping(map, adopt, fragment)
        }
        else {
          parseSingleNodeMapping(map, adopt, fragment)
        }

      // 2) reference linking a declared node mapping
      case YType.Str if entry.value.toOption[YScalar].isDefined =>
        resolveNodeMappingLink(entry, adopt)

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

      case _ =>
        Some(NodeMapping(Annotations(entry.value)))
    }
  }

  def checkGuidUniqueContraint(propertyMapping: PropertyMapping, ast: YPart): Unit = {
    propertyMapping.literalRange().option() foreach { literal =>
      if (literal == (Namespace.Shapes + "guid").iri() && !propertyMapping.unique().option().getOrElse(false)) {
        ctx.eh.warning(
            DialectValidations.GuidRangeWithoutUnique,
            propertyMapping.id,
            s"Declaration of property '${propertyMapping.name().value()}' with range GUID and without unique constraint",
            ast
        )
      }
    }
  }

  def parsePropertyMapping(entry: YMapEntry, adopt: PropertyMapping => Any): PropertyMapping = {
    val name = ScalarNode(entry.key).string()
    entry.value.tagType match {
      case YType.Map =>
        val map             = entry.value.as[YMap]
        val propertyMapping = PropertyMapping(map).set(PropertyMappingModel.Name, name, Annotations(entry.key))

        adopt(propertyMapping)
        ctx.closedNode("propertyMapping", propertyMapping.id, map)

        map.key("propertyTerm") match {
          case Some(e) =>
            val value          = ValueNode(e.value)
            val propertyTermId = value.string().toString
            ctx.declarations.findPropertyTerm(propertyTermId, SearchScope.All) match {
              case Some(propertyTerm) =>
                propertyMapping.withNodePropertyMapping(propertyTerm.id)
              case _ =>
                ctx.eh.violation(DialectError,
                                 propertyMapping.id,
                                 s"Cannot find property term with alias $propertyTermId",
                                 e.value)
            }
          case _ =>
            propertyMapping.withNodePropertyMapping(
                (Namespace.Data + entry.key.as[YScalar].text.urlComponentEncoded).iri())
        }

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
                    case "guid" =>
                      propertyMapping.withLiteralRange((Namespace.Shapes + "guid").iri())
                    case "string" | "integer" | "boolean" | "float" | "decimal" | "double" | "duration" | "dateTime" |
                        "time" | "date" | "anyType" =>
                      propertyMapping.withLiteralRange((Namespace.Xsd + range).iri())
                    case "anyUri"  => propertyMapping.withLiteralRange(DataType.AnyUri)
                    case "link"    => propertyMapping.withLiteralRange((Namespace.Shapes + "link").iri())
                    case "number"  => propertyMapping.withLiteralRange(DataType.Number)
                    case "uri"     => propertyMapping.withLiteralRange(DataType.AnyUri)
                    case "any"     => propertyMapping.withLiteralRange(DataType.Any)
                    case "anyNode" => propertyMapping.withObjectRange(Seq((Namespace.Meta + "anyNode").iri()))
                    case nodeMappingId =>
                      propertyMapping
                        .withObjectRange(Seq(nodeMappingId)) // temporary until we can resolve all nodeMappings after finishing parsing declarations
                  }
              }
            }
        )

        parseMapKey(map, propertyMapping)

        parseMapValue(map, propertyMapping)

        map.key(
            "patch",
            entry => {
              val patchMethod = ScalarNode(entry.value).string()
              propertyMapping.set(PropertyMappingModel.MergePolicy, patchMethod, Annotations(entry))
              val patchMethodValue = patchMethod.toString
              if (!MergePolicies.isAllowed(patchMethodValue)) {
                ctx.eh.violation(DialectError,
                                 propertyMapping.id,
                                 s"Unsupported propertu mapping patch operation '$patchMethod'",
                                 entry.value)
              }
            }
        )

        map.key(
            "mandatory",
            entry => {
              val required = ScalarNode(entry.value).boolean().toBool
              val value    = if (required) 1 else 0
              propertyMapping.set(PropertyMappingModel.MinCount,
                                  AmfScalar(value, Annotations(entry.value)),
                                  Annotations(entry))
            }
        )

        map.parse("pattern", propertyMapping setParsing PropertyMappingModel.Pattern)
        map.parse("minimum", propertyMapping setParsing PropertyMappingModel.Minimum)
        map.parse("unique", propertyMapping setParsing PropertyMappingModel.Unique)
        map.parse("maximum", propertyMapping setParsing PropertyMappingModel.Maximum)

        map.parse("allowMultiple", propertyMapping setParsing PropertyMappingModel.AllowMultiple)
        map.parse("sorted", propertyMapping setParsing PropertyMappingModel.Sorted)

        map.key(
            "enum",
            entry => {
              val seq = entry.value.as[YSequence]
              val values = seq.nodes.flatMap { node =>
                node.value match {
                  case scalar: YScalar => Some(ScalarNode(node).string())
                  case _ =>
                    ctx.eh.violation(DialectError, "Cannot create enumeration constraint from not scalar value", node)
                    None
                }
              }
              propertyMapping.set(PropertyMappingModel.Enum, AmfArray(values, Annotations(seq)), Annotations(entry))
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

        map.parse("typeDiscriminatorName", propertyMapping setParsing PropertyMappingModel.TypeDiscriminatorName)

        // TODO: check dependencies among properties

        map.key(
            "isLink",
            entry => {
              val isLink = entry.value.as[Boolean]
              propertyMapping.withExternallyLinkable(isLink);
              propertyMapping.literalRange().option() match {
                case Some(v) =>
                  ctx.eh.violation(
                      DialectError,
                      s"Aml links support in property mappings only can be declared in object properties but scalar range detected: ${v}",
                      entry.value)
                case _ => // ignore
              }
            }
        )

        parseAnnotations(map, propertyMapping, ctx.declarations)

        // We check that if this is a GUID it also has the unique contraint
        checkGuidUniqueContraint(propertyMapping, map)

        propertyMapping
      case _ =>
        PropertyMapping(Annotations(entry)).set(PropertyMappingModel.Name, name, Annotations(entry.key))
    }
  }

  private def parseMapKey(map: YMap, propertyMapping: PropertyMapping): Unit = {
    val mapKey     = map.key("mapKey")
    val mapTermKey = map.key("mapTermKey")

    for {
      _ <- mapKey
      _ <- mapTermKey
    } yield {
      ctx.eh.violation(DialectError, propertyMapping.id, s"mapKey and mapTermKey are mutually exclusive", map)
    }

    mapTermKey.fold({
      mapKey.foreach(entry => {
        val propertyLabel = ValueNode(entry.value).string().toString
        propertyMapping.withMapKeyProperty(propertyLabel)
      })
    })(entry => {
      val propertyTermId = ValueNode(entry.value).string().toString
      getTermIfValid(propertyTermId, propertyMapping.id, entry.value).foreach(propertyMapping.withMapTermKeyProperty)
    })
  }

  private def parseMapValue(map: YMap, propertyMapping: PropertyMapping): Unit = {
    val mapValu      = map.key("mapValue")
    val mapTermValue = map.key("mapTermValue")

    for {
      _ <- mapValu
      _ <- mapTermValue
    } yield {
      ctx.eh.violation(DialectError, propertyMapping.id, s"mapValue and mapTermValue are mutually exclusive", map)
    }

    mapTermValue.fold({
      mapValu.foreach(entry => {
        val propertyLabel = ValueNode(entry.value).string().toString
        propertyMapping.withMapValueProperty(propertyLabel)
      })
    })(entry => {
      val propertyTermId = ValueNode(entry.value).string().toString
      getTermIfValid(propertyTermId, propertyMapping.id, entry.value).foreach(propertyMapping.withMapTermValueProperty)
    })

  }

  private def getTermIfValid(iri: String, propertyMappingId: String, ast: YPart): Option[String] = {
    Namespace(iri).base match {
      case Namespace.Data.base => Some(iri)
      case _ =>
        ctx.declarations.findPropertyTerm(iri, All) match {
          case Some(term) => Some(term.id)
          case _ =>
            ctx.eh.violation(DialectError, propertyMappingId, s"Cannot find property term with alias $iri", ast)
            None
        }
    }
  }

  def validateTemplate(template: String, map: YMap, propMappings: Seq[PropertyMapping]): Unit = {
    val regex = "(\\{[^}]+\\})".r
    regex.findAllIn(template).foreach { varMatch =>
      val variable = varMatch.replace("{", "").replace("}", "")
      propMappings.find(_.name().value() == variable) match {
        case Some(prop) =>
          if (prop.minCount().option().getOrElse(0) != 1)
            ctx.eh.violation(DialectError,
                             prop.id,
                             s"PropertyMapping for idTemplate variable '$variable' must be mandatory",
                             map)
        case None =>
          ctx.eh.violation(DialectError, "", s"Missing propertyMapping for idTemplate variable '$variable'", map)
      }
    }
  }

  private def parseDocumentsMapping(map: YMap): Unit = {
    map.key("documents").foreach { e =>
      val doc =
        DocumentsModelParser(e.value, dialect.id, s"${dialect.name().value()} ${dialect.version().value()}").parse()
      dialect.set(DialectModel.Documents, doc, Annotations(e))
    }
  }

  def parseLibrary(): BaseUnit = {
    map.parse("usage", dialect setParsing DialectModel.Usage)

    // closed node validation
    ctx.closedNode("library", dialect.id, map)

    val references = DialectsReferencesParser(dialect, map, root.references).parse()

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

    map.parse("usage", dialect setParsing DialectModel.Usage)

    // closed node validation
    ctx.closedNode("fragment", dialect.id, map)

    val references = DialectsReferencesParser(dialect, map, root.references).parse()

    if (ctx.declarations.externals.nonEmpty)
      dialect.withExternals(ctx.declarations.externals.values.toSeq)

    parseDeclarations(root, map)

    if (references.baseUnitReferences().nonEmpty) dialect.withReferences(references.baseUnitReferences())

    val fragment = toFragment(dialect)

    parseNodeMapping(
        YMapEntry(YNode("fragment"), map), {
          case mapping: NodeMapping      => mapping.withId(fragment.id + "/fragment").withName("fragment")
          case mapping: UnionNodeMapping => mapping.withId(fragment.id + "/fragment").withName("fragment")
        },
        fragment = true
    ) match {
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

  protected def resolveNodeMappingLink(entry: YMapEntry, adopt: DomainElement => Any): Some[NodeMapping] = {
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
        adopt(linkedNode) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
        Some(linkedNode)
      case (text: String, _) =>
        val linkedNode = NodeMapping(map)
        adopt(linkedNode)
        linkedNode.unresolved(text, map)
        Some(linkedNode)
    }
  }
}
