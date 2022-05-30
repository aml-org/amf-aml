package amf.aml.internal.parse.dialects

import amf.aml.client.scala.model.document.{Dialect, DialectFragment, DialectLibrary}
import amf.aml.client.scala.model.domain._
import amf.aml.internal.metamodel.document.DialectModel
import amf.aml.internal.metamodel.document.DialectModel.Externals
import amf.aml.internal.metamodel.domain.UnionNodeMappingModel.ObjectRange
import amf.aml.internal.metamodel.domain.{NodeMappingModel, PropertyMappingModel}
import amf.aml.internal.parse.common.{DeclarationKey, DeclarationKeyCollector}
import amf.aml.internal.parse.dialects.DialectAstOps._
import amf.aml.internal.parse.dialects.nodemapping.like.NodeMappingLikeParser
import amf.aml.internal.parse.dialects.property.like.AnnotationMappingParser
import amf.aml.internal.validate.DialectValidations.{DialectError, EventualAmbiguity, UnavoidableAmbiguity}
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar, DomainElement}
import amf.core.client.scala.parse.AMFParser
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.annotations._
import amf.core.internal.metamodel.document.FragmentModel
import amf.core.internal.parser.domain.SearchScope.All
import amf.core.internal.parser.domain._
import amf.core.internal.parser.{Root, YNodeLikeOps}
import amf.core.internal.remote.Spec.AML
import amf.core.internal.utils._
import org.yaml.model._

import scala.collection.mutable

class DialectsParser(root: Root)(implicit override val ctx: DialectContext)
    extends BaseSpecParser
    with DeclarationKeyCollector {

  type NodeMappable = NodeMappable.AnyNodeMappable
  val map: YMap = root.parsed.asInstanceOf[SyamlParsedDocument].document.as[YMap]
  val dialect: Dialect = {
    val computedId = id()
    val dialect    = Dialect(Annotations(map)).withLocation(root.location).withId(computedId)
    dialect.processingData.adopted(computedId + "#")
    dialect
  }

  // Need to do this before parsing so every ID set during parsing is relative to this ID
  private def id(): String = {
    map.key("$id") match {
      case Some(entry) =>
        entry.value.tagType match {
          case YType.Str => entry.value.toString
          case t =>
            val defaultId = defaultDialectId()
            ctx.eh
              .violation(
                  DialectError,
                  defaultId,
                  s"Invalid type $t for $$id directive. Expected ${YType.Str}",
                  entry.value.location
              )
            defaultId
        }
      case None =>
        root.location match {
          case AMFParser.DEFAULT_DOCUMENT_URL => defaultDialectId()
          case location                       => location
        }
    }
  }

  private def defaultDialectId(): String = {
    val nameAndVersionId = for {
      dialectEntry <- map.key("dialect")
      versionEntry <- map.key("version")
    } yield {
      val dialect = dialectEntry.value.toString.toLowerCase.noSpaces
      val version = versionEntry.value.toString.toLowerCase.noSpaces
      s"${AMFParser.DEFAULT_DOCUMENT_URL}/$dialect/$version"
    }

    nameAndVersionId
      .getOrElse(AMFParser.DEFAULT_DOCUMENT_URL) // Name and version will always be defined otherwise dialect is invalid
  }

  def parseSemanticExtensions(map: YMap): Unit = {
    map.key("extensions").foreach { extensionsEntry =>
      extensionsEntry.value.tagType match {
        case YType.Map =>
          val entries = extensionsEntry.value.as[YMap].entries
          val semanticExtensions =
            entries.map(e => SemanticExtensionParser(e, s"${dialect.id}/semantic-extensions").parse())
          dialect.setArrayWithoutId(
              DialectModel.Extensions,
              semanticExtensions,
              Annotations(extensionsEntry) ++= Annotations.virtual()
          )
        case t =>
          ctx.eh.violation(
              DialectError,
              dialect.id,
              s"Invalid type $t for 'extensions' node. Expected map",
              extensionsEntry.value
          )
      }
    }
  }

  def parseDocument(): BaseUnit = {

    map.parse("dialect", dialect setParsing DialectModel.Name)
    map.parse("usage", dialect setParsing DialectModel.Usage)
    map.parse("version", dialect setParsing DialectModel.Version)

    // closed node validation
    ctx.closedNode("dialect", dialect.id, map)

    val references = DialectsReferencesParser(dialect, map, root.references).parse()

    if (ctx.declarations.externals.nonEmpty) {
      val entry = map.key("external").get
      dialect.set(
          Externals,
          AmfArray(ctx.declarations.externals.values.toSeq, Annotations(entry.value)),
          Annotations(entry)
      )
    }

    parseDeclarations(dialect.id, map)

    val declarables = ctx.declarations.declarables()
    declarables.foreach {
      case anyMapping: AnyMapping               => checkAnyNodeMappableReferences(anyMapping)
      case annotationMapping: AnnotationMapping => checkNodeMappableReferences(annotationMapping)
    }
    addDeclarationsToModel(dialect)
    if (references.baseUnitReferences().nonEmpty) dialect.withReferences(references.baseUnitReferences())

    parseDocumentsMapping(map)
    parseSemanticExtensions(map)

    // resolve unresolved references
    dialect.declares.foreach {
      case dec: Member =>
        if (!dec.isUnresolved) {
          ctx.futureDeclarations.resolveRef(dec.name.value(), dec)
        }
      case _ => //
    }
    ctx.futureDeclarations.resolve()

    dialect.processingData.withSourceSpec(AML)
    dialect
  }

  def parseAnnotationMappingDeclarations(map: YMap, parent: String): Unit = {
    map.key("annotationMappings").foreach { e =>
      addDeclarationKey(DeclarationKey(e, isAbstract = true))
      e.value.tagType match {
        case YType.Map =>
          e.value.as[YMap].entries.foreach { entry =>
            ctx.declarations += AnnotationMappingParser(entry, parent).parse()
          }
        case YType.Null =>
        case t =>
          ctx.eh.violation(DialectError, parent, s"Invalid type $t for 'annotationMappings' node", e.value.location)
      }
    }
  }

  protected def parseDeclarations(baseIri: String, map: YMap): Unit = {
    val parent = baseIri + "#/declarations"
    parseNodeMappingDeclarations(map, parent)
    parseAnnotationMappingDeclarations(map, parent)
  }

  /** Recursively collects members from unions and nested unions
    * @param union
    *   union to extract members from
    * @param path
    *   path to show error on nested ambiguities
    * @return
    *   Map member -> path from root level union
    */
  private def flattenedMembersFrom(
      union: NodeWithDiscriminator[_],
      path: Seq[UnionNodeMapping] = Nil
  ): Map[NodeMapping, Seq[UnionNodeMapping]] = {
    membersFrom(union).flatMap {
      case anotherUnion: UnionNodeMapping if !path.contains(union) =>
        flattenedMembersFrom(
            anotherUnion,
            path :+ anotherUnion
        ) // if member is another union get its members recursively
      case nodeMapping: NodeMapping => Some(nodeMapping -> path)
      case _                        => None
    }.toMap
  }

  protected def membersFrom(union: NodeWithDiscriminator[_]): Seq[NodeMappable] = {
    union
      .objectRange()
      .toStream
      .flatMap { name =>
        ctx.declarations.findNodeMapping(name.value(), All)
      }
  }

  type Member            = NodeMapping
  type ConcatenatedNames = String

  /** Ambiguity kinds:
    *   - Un-avoidable: cannot distinguish between two union members
    *   - Eventual: some times cannot distinguish between two union members, depending on the value of the dialect
    *     instance
    *
    * Conditions for un-avoidable ambiguity:
    *   - Set of property names is exactly the same between two union members
    *
    * Conditions for eventual ambiguity:
    *   - Set of MANDATORY property names is exactly the same between two union members
    *
    * @param union
    *   Union to calculate ambiguity over its members
    * @tparam T
    *   type of union: union node mapping or union property mapping
    */
  def checkAmbiguity[T <: DomainElement](union: NodeWithDiscriminator[_]): Unit = {
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
        ctx.eh.violation(
            UnavoidableAmbiguity,
            union.id,
            s"Union is ambiguous. Members $names have the same set of property names",
            union.annotations
        )
      case _ => // Ignore
    }

    eventualCache.foreach {
      case (_, members) if members.size > 1 =>
        val names = members.map(buildPath).sorted.mkString(", ")
        ctx.eh.warning(
            EventualAmbiguity,
            union.id,
            s"Union might be ambiguous. Members $names have the same set of mandatory property names",
            union.annotations
        )
      case _ => // Ignore
    }
  }

  private def updateAmbiguityCaches[T <: DomainElement](
      member: Member,
      unavoidableCache: mutable.HashMap[ConcatenatedNames, Seq[Member]],
      eventualCache: mutable.HashMap[ConcatenatedNames, Seq[Member]]
  ) = {

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

  private def findIdOrError(value: StrField, mappable: AnyMapping): Option[String] = {
    val name = value.toString
    // Is necessary this check? It always should be a reference
    if (name == (Namespace.Meta + "anyNode").iri()) Some(name)
    else {
      ctx.declarations.findNodeMapping(name, All) match {
        case Some(mapping) => Some(mapping.id)
        case None =>
          ctx.findInRecursiveUnits(name) match {
            case Some(recursiveId) => Some(recursiveId)
            case None =>
              ctx.missingPropertyRangeViolation(
                  name,
                  mappable.id,
                  value.annotations()
              )
              None
          }
      }
    }
  }

  protected def checkAnyNodeMappableReferences(mappable: AnyMapping): Unit = {

    val allMembers = mappable.and.flatMap(member => findIdOrError(member, mappable))
    if (allMembers.nonEmpty) mappable.withAnd(allMembers)
    val oneMembers = mappable.or.flatMap(member => findIdOrError(member, mappable))
    if (oneMembers.nonEmpty) mappable.withOr(oneMembers)
    val components = mappable.components.flatMap(member => findIdOrError(member, mappable))
    if (components.nonEmpty) mappable.withComponents(components)
    if (mappable.ifMapping.nonEmpty)
      findIdOrError(mappable.ifMapping, mappable).foreach(mappable.withIfMapping)
    if (mappable.thenMapping.nonEmpty)
      findIdOrError(mappable.thenMapping, mappable).foreach(mappable.withThenMapping)
    if (mappable.elseMapping.nonEmpty)
      findIdOrError(mappable.elseMapping, mappable).foreach(mappable.withElseMapping)

    mappable match {
      case unionNodeMapping: UnionNodeMapping => checkNodeMappableReferences(unionNodeMapping)
      case nodeMapping: Member =>
        nodeMapping.propertiesMapping().foreach { propertyMapping =>
          checkNodeMappableReferences(propertyMapping)
        }
    }

  }

  /** This method:
    *   1. replaces union & discriminator members references to other node mappings from their name to their id 2.
    *      validates union & discriminator members exist 3. checks for ambiguity
    */
  protected def checkNodeMappableReferences[T <: DomainElement](mappable: NodeWithDiscriminator[_]): Unit = {
    val memberStream = mappable.objectRange().toStream
    val memberIdsStream = memberStream
      .map(field => (memberIdFromName(field.value(), mappable), field.annotations()))
      .filter { case (optionalValue, _) =>
        optionalValue.isDefined
      }
      .map(t => AmfScalar(t._1.get, t._2))

    mappable match {
      case p: PropertyMapping if p.isUnion && p.typeDiscriminatorName().option().isEmpty => checkAmbiguity(p)
      case u: UnionNodeMapping if u.typeDiscriminatorName().option().isEmpty             => checkAmbiguity(u)
      case _                                                                             => // Ignore
    }
    if (memberIdsStream.nonEmpty) {
      val objectRangeValue = mappable.fields.getValue(ObjectRange)
      mappable.set(
          ObjectRange,
          AmfArray(memberIdsStream, objectRangeValue.value.annotations),
          objectRangeValue.annotations
      )
    }

    // Setting ids we left unresolved in typeDiscriminators
    Option(mappable.typeDiscriminator()) match {
      case Some(typeDiscriminators) =>
        val fieldValue = mappable.fields.entry(PropertyMappingModel.TypeDiscriminator).map(_.value)
        val discriminatorValueMapping = typeDiscriminators.flatMap { case (name, discriminatorValue) =>
          ctx.declarations
            .findNodeMapping(name, All)
            .map(_.id -> discriminatorValue)
            .orElse {
              ctx.missingPropertyRangeViolation(
                  name,
                  mappable.id,
                  fieldValue
                    .map(_.annotations)
                    .getOrElse(mappable.annotations)
              )
              None
            }
        }
        mappable.withTypeDiscriminator(
            discriminatorValueMapping,
            fieldValue
              .map(_.annotations)
              .getOrElse(Annotations()),
            fieldValue
              .map(_.value.annotations)
              .getOrElse(Annotations())
        )
      case _ => // ignore
    }
  }

  private def memberIdFromName[T <: DomainElement](name: String, union: NodeWithDiscriminator[_]): Option[String] = {
    if (name == (Namespace.Meta + "anyNode").iri()) Some(name)
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
            case Some(recursiveId) => Some(recursiveId)
            case _ =>
              val lexicalRange: Annotations = union
                .objectRange()
                .find(p => p.value() == name)
                .map(p => p.annotations())
                .orElse(
                    union.fields
                      .entry(PropertyMappingModel.ObjectRange)
                      .map(_.value.annotations)
                )
                .getOrElse(union.annotations)
              ctx.missingPropertyRangeViolation(
                  name,
                  union.id,
                  lexicalRange
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
              ctx.differentTermsInMapKey(
                  propertyMapping.id,
                  PropertyMappingModel.MapKeyProperty.value.iri(),
                  label,
                  propertyMapping.mapKeyProperty().annotations()
              )
              propertyMapping.fields.removeField(PropertyMappingModel.MapTermKeyProperty)
              propertyMapping.fields.removeField(PropertyMappingModel.MapKeyProperty)
            case _ => propertyMapping.withMapTermKeyProperty(term)
          }
        case _ =>
          ctx.missingPropertyKeyViolation(
              propertyMapping.id,
              PropertyMappingModel.MapKeyProperty.value.iri(),
              label,
              propertyMapping.mapKeyProperty().annotations()
          )
          propertyMapping.fields.removeField(PropertyMappingModel.MapKeyProperty)
      }
    }
  }

  private def parseNodeMappingDeclarations(map: YMap, parent: String): Unit = {
    map.key("nodeMappings").foreach { e =>
      addDeclarationKey(DeclarationKey(e, isAbstract = true))
      e.value.tagType match {
        case YType.Map =>
          e.value.as[YMap].entries.foreach { entry =>
            val nodeName = entry.key.toString
            if (AmlScalars.all.contains(nodeName)) {
              ctx.eh
                .violation(
                    DialectError,
                    parent,
                    s"Error parsing node mapping: '$nodeName' is a reserved name",
                    entry.location
                )
            } else {
              val adopt: DomainElement => Any = {
                case nodeMapping: NodeMappable =>
                  val name = ScalarNode(entry.key).string()
                  nodeMapping.set(NodeMappingModel.Name, name, Annotations(entry.key)).adopted(parent)
                  nodeMapping.annotations.reject(a =>
                    a.isInstanceOf[SourceAST] ||
                      a.isInstanceOf[LexicalInformation] ||
                      a.isInstanceOf[SourceLocation] ||
                      a.isInstanceOf[SourceNode]
                  )
                  nodeMapping.annotations ++= Annotations(entry)
                case _ =>
                  ctx.eh.violation(
                      DialectError,
                      parent,
                      s"Error only valid node mapping or union mapping can be declared",
                      entry.location
                  )
                  None
              }

              parseNodeMapping(entry, adopt) match {
                case Some(nodeMapping: NodeMapping)       => ctx.declarations += nodeMapping
                case Some(unionMapping: UnionNodeMapping) => ctx.declarations += unionMapping
                case _ => ctx.eh.violation(DialectError, parent, s"Error parsing shape '$entry'", entry.location)
              }

            }
          }
        case YType.Null =>
        case t => ctx.eh.violation(DialectError, parent, s"Invalid type $t for 'nodeMappings' node.", e.value.location)
      }
    }
  }

  def parseNodeMapping(
      entry: YMapEntry,
      adopt: DomainElement => Any,
      fragment: Boolean = false
  ): Option[NodeMappable] = {
    entry.value.tagType match {
      // 1) inlined node mapping
      case YType.Map =>
        val inlinedNodeMappable = NodeMappingLikeParser.parse(entry, adopt, fragment)
        Some(inlinedNodeMappable)

      // 2) reference linking a declared node mapping
      case YType.Str if entry.value.toOption[YScalar].isDefined =>
        NodeMappingLikeParser.resolveNodeMappingLink(map, entry.value, adopt)

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
            adopt(
                linkedNode
            ) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            Some(linkedNode)
          case (text: String, _) =>
            val nodeMappingTmp = adopt(NodeMapping()).asInstanceOf[NodeMapping]
            val suffix         = nodeMappingTmp.id.split("#").head
            ctx.missingFragmentViolation(text, nodeMappingTmp.id.replace(suffix, ""), entry.value)
            None
        }

      case _ => Some(NodeMapping(Annotations(entry.value)))
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

    parseDeclarations(dialect.id, map)

    val declarables = ctx.declarations.declarables()
    declarables.foreach {
      case unionNodeMapping: UnionNodeMapping =>
        checkNodeMappableReferences(unionNodeMapping)

      case nodeMapping: NodeMapping =>
        nodeMapping.propertiesMapping().foreach { propertyMapping =>
          checkNodeMappableReferences(propertyMapping)
        }

      case annotationMapping: AnnotationMapping => checkNodeMappableReferences(annotationMapping)
    }
    addDeclarationsToModel(dialect)
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
    addDeclarationsToModel(library, declares)

    val externals = dialect.externals
    if (externals.nonEmpty) library.withExternals(externals)

    library.processingData.withSourceSpec(AML)
    library
  }

  def parseFragment(): BaseUnit = {

    map.parse("usage", dialect setParsing DialectModel.Usage)

    // closed node validation
    ctx.closedNode("fragment", dialect.id, map)

    val references = DialectsReferencesParser(dialect, map, root.references).parse()

    if (ctx.declarations.externals.nonEmpty)
      dialect.withExternals(ctx.declarations.externals.values.toSeq)

    parseDeclarations(dialect.id, map)

    if (references.baseUnitReferences().nonEmpty) dialect.withReferences(references.baseUnitReferences())

    val fragment = toFragment(dialect)

    parseNodeMapping(
        YMapEntry(YNode("fragment"), map),
        {
          case mapping: NodeMapping      => mapping.withId(fragment.id + "/fragment").withName("fragment")
          case mapping: UnionNodeMapping => mapping.withId(fragment.id + "/fragment").withName("fragment")
        },
        fragment = true
    ) match {
      case Some(encoded: DomainElement) =>
        fragment.fields.setWithoutId(FragmentModel.Encodes, encoded, Annotations.inferred())
      case _ => // ignore
    }

    fragment.processingData.withSourceSpec(AML)
    fragment
  }

  protected def toFragment(dialect: Dialect): DialectFragment = {
    val fragment = DialectFragment(dialect.annotations)
      .withId(dialect.id)
      .withLocation(dialect.location().getOrElse(dialect.id))
      .withReferences(dialect.references)
    fragment.processingData.adopted(dialect.id + "#")

    dialect.usage.option().foreach(usage => fragment.withUsage(usage))

    val externals = dialect.externals
    if (externals.nonEmpty) fragment.withExternals(dialect.externals)

    fragment
  }

}
