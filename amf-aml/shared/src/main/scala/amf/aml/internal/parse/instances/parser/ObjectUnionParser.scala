package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain._
import amf.aml.internal.annotations._
import amf.aml.internal.parse.instances.ClosedInstanceNode.checkNode
import amf.aml.internal.parse.instances.InstanceNodeIdHandling.generateNodeId
import amf.aml.internal.parse.instances.finder.{IncludeFirstUnionElementFinder, JSONPointerUnionFinder}
import amf.aml.internal.parse.instances.parser.ExternalLinkGenerator.PropertyParser
import amf.aml.internal.parse.instances.{DialectInstanceContext, DialectInstanceParser, DialectInstanceParserOps}
import amf.aml.internal.validate.DialectValidations.{DialectAmbiguousRangeSpecification, DialectError, InvalidUnionType}
import amf.core.client.scala.model.domain.{Annotation, DomainElement}
import amf.core.internal.parser.Root
import amf.core.internal.parser.domain.Annotations
import org.yaml.model._

object ObjectUnionParser {

  def parse[T <: DomainElement](
      defaultId: String,
      path: Seq[String],
      ast: YNode,
      unionMapping: NodeWithDiscriminator[_],
      additionalProperties: Map[String, Any] = Map(),
      root: Root,
      rootMap: YMap,
      propertyParser: PropertyParser
  )(implicit ctx: DialectInstanceContext): DialectDomainElement = {

    // potential node range based in the objectRange
    val unionMembers: Seq[NodeMapping] = findUnionMembers(defaultId, unionMapping, ast)

    // potential node range based in discriminators map
    val discriminatorsMapping: Map[String, NodeMapping] = findDiscriminatorMappings(unionMapping, defaultId, ast)

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

        DialectInstanceParserOps.computeParsingScheme(nodeMap) match {
          case "$include" =>
            val link = resolveLinkUnion(ast, allPossibleMappings, defaultId, rootMap)
            link.annotations += RefInclude()
            link
          case "$ref" =>
            val ref = resolveJSONPointerUnion(nodeMap, allPossibleMappings, defaultId)
            ref.annotations += JsonPointerRef()
            ref
          case _ =>
            val discriminatorName = unionMapping.typeDiscriminatorName().option()
            val mappings = findCompatibleMapping(
              defaultId,
              unionMembers,
              discriminatorsMapping,
              discriminatorName,
              nodeMap,
              additionalProperties.keys.toSeq
            )
            if (mappings.isEmpty) {
              ctx.eh.violation(
                DialectAmbiguousRangeSpecification,
                defaultId,
                s"Ambiguous node in union range, found 0 compatible mappings from ${allPossibleMappings.size} mappings: [${allPossibleMappings.map(_.id).mkString(",")}]",
                ast.location
              )
              DialectDomainElement(annotations += FromUnionRangeMapping(allPossibleMappings.map(_.id)))
                .withId(defaultId)
            } else if (mappings.size == 1) {
              val node: DialectDomainElement = DialectDomainElement(annotations).withDefinedBy(mappings.head)
              val finalId =
                generateNodeId(
                  node,
                  nodeMap,
                  path,
                  defaultId,
                  mappings.head,
                  additionalProperties,
                  rootNode = false,
                  root
                )
              node.withId(finalId)
              var instanceTypes: Seq[String] = Nil
              mappings.foreach { mapping =>
                val beforeValues = node.fields.fields().size
                mapping.propertiesMapping().foreach { propertyMapping =>
                  if (!node.containsProperty(propertyMapping)) {
                    val propertyName = propertyMapping.name().value()

                    nodeMap.entries.find(_.key.as[YScalar].text == propertyName) match {
                      case Some(entry) => propertyParser(finalId, entry, propertyMapping, node)
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
              checkNodeForAdditionalKeys(
                finalId,
                mappings.head.id,
                nodeMap.map,
                mappings.head,
                nodeMap,
                rootNode = false,
                discriminatorName
              )
              node
            } else {
              ctx.eh.violation(
                DialectAmbiguousRangeSpecification,
                defaultId,
                None, // Some(property.nodePropertyMapping().value()),
                s"Ambiguous node, please provide a type disambiguator. Nodes ${mappings.map(_.id).mkString(",")} have been found compatible, only one is allowed",
                rootMap.location
              )
              DialectDomainElement(annotations)
                .withId(defaultId)
            }
        }

      case YType.Str | YType.Include => // here the mapping information is explicit in the fragment/declaration mapping
        resolveLinkUnion(ast, allPossibleMappings, defaultId, rootMap)

      case _ =>
        ctx.eh.violation(InvalidUnionType, defaultId, "Cannot parse AST for union node mapping", ast.location)
        DialectDomainElement(Annotations(ast) += FromUnionRangeMapping(allPossibleMappings.map(_.id)))
          .withId(defaultId)
    }
  }

  private def findDiscriminatorMappings[T <: DomainElement](
      unionMapping: NodeWithDiscriminator[_],
      defaultId: String,
      ast: YPart
  )(implicit ctx: DialectInstanceContext): Map[String, NodeMapping] = {

    Option(unionMapping.typeDiscriminator()) match {
      case Some(discriminatorValueMapping) =>
        discriminatorValueMapping.flatMap { case (discriminatorValue, nodeMappingId) =>
          ctx.dialect.declares.find(_.id == nodeMappingId) match {
            case Some(nodeMapping: NodeMapping) => Some(discriminatorValue -> nodeMapping)
            case _ =>
              ctx.eh.violation(
                DialectError,
                defaultId,
                s"Cannot find mapping for property $nodeMappingId in discriminator value '$discriminatorValue' in union",
                ast.location
              )
              None
          }
        }
      case None =>
        Map.empty
    }
  }

  private def findUnionMembers[T <: DomainElement](
      defaultId: String,
      unionMapping: NodeWithDiscriminator[_],
      ast: YPart
  )(implicit ctx: DialectInstanceContext) = {
    unionMapping.objectRange().flatMap { memberId =>
      ctx.dialect.declares.find(_.id == memberId.value()) match {
        case Some(nodeMapping: NodeMapping) => Some(nodeMapping)
        case _ =>
          ctx.eh
            .violation(
              DialectError,
              defaultId,
              s"Cannot find mapping for property ${unionMapping.id} in union",
              ast.location
            )
          None
      }
    }
  }

  def findCompatibleMapping(
      id: String,
      unionMappings: Seq[NodeMapping],
      discriminatorsMapping: Map[String, NodeMapping],
      discriminator: Option[String],
      nodeMap: YMap,
      additionalProperties: Seq[String]
  )(implicit ctx: DialectInstanceContext): Seq[NodeMapping] = {
    discriminator match {
      // Using explicit discriminator
      case Some(propertyName) =>
        val explicitMapping = findExplicitMapping(nodeMap, propertyName, discriminatorsMapping)
        explicitMapping match {
          case Some(nodeMapping) => Seq(nodeMapping)
          case None =>
            ctx.eh.violation(
              DialectError,
              id,
              s"Cannot find discriminator value for discriminator '$propertyName'",
              nodeMap.location
            )
            Nil
        }
      // Inferring based on properties
      case None =>
        val properties: Set[String] = keys(nodeMap).filter(isRegularKey)
        unionMappings.filter { mapping =>
          val baseProperties =
            mapping.propertiesMapping().filter(pm => !additionalProperties.contains(pm.nodePropertyMapping().value()))
          val mappingRequiredSet: Set[String] = requiredProperties(baseProperties)
          val mappingSet: Set[String] = baseProperties
            .map(_.name().value())
            .toSet

          // There are not additional properties in the set and all required properties are in the set
          val canParseAllProperties    = properties.diff(mappingSet).isEmpty
          val hasAllRequiredProperties = mappingRequiredSet.diff(properties).isEmpty
          canParseAllProperties && hasAllRequiredProperties
        }
    }
  }

  private def findExplicitMapping(
      nodeMap: YMap,
      propertyName: String,
      discriminatorsMapping: Map[String, NodeMapping]
  ) = {
    nodeMap.entries.find(_.key.as[YScalar].text == propertyName).flatMap { entry =>
      discriminatorsMapping.get(entry.value.as[YScalar].text)
    }
  }

  private def requiredProperties(baseProperties: Seq[PropertyMapping]) = {
    baseProperties
      .filter(_.minCount().value() > 0)
      .map(_.name().value())
      .toSet
  }

  private def keys(nodeMap: YMap) = nodeMap.entries.map(_.key.as[YScalar].text).toSet

  private def isRegularKey = (x: String) => !x.startsWith("$")

  private def discriminatorAnnotation(discriminatorName: Option[String], nodeMap: YMap)(implicit
      ctx: DialectInstanceContext
  ): Option[Annotation] = {
    discriminatorName.flatMap { propertyName =>
      nodeMap.entries.find(_.key.as[YScalar].text == propertyName).map { entry =>
        DiscriminatorField(propertyName, entry.value.as[YScalar].text)
      }
    }
  }

  protected def resolveLinkUnion(ast: YNode, allPossibleMappings: Seq[NodeMapping], id: String, root: YMap)(implicit
      ctx: DialectInstanceContext
  ): DialectDomainElement = {
    IncludeFirstUnionElementFinder.find(ast, allPossibleMappings, id, root)
  }

  protected def resolveJSONPointerUnion(map: YMap, allPossibleMappings: Seq[NodeMapping], id: String)(implicit
      ctx: DialectInstanceContext
  ): DialectDomainElement = {
    JSONPointerUnionFinder.find(map, allPossibleMappings, id, map)
  }

  private def checkNodeForAdditionalKeys(
      id: String,
      nodetype: String,
      entries: Map[YNode, YNode],
      mapping: NodeMapping,
      ast: YPart,
      rootNode: Boolean,
      additionalKey: Option[String]
  )(implicit ctx: DialectInstanceContext): Unit = {
    checkNode(id, nodetype, entries, mapping, ast, rootNode, additionalKey)
  }
}
