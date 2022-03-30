package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain._
import amf.aml.internal.parse.common.AnnotationsParser.parseAnnotations
import amf.aml.internal.parse.instances.ClosedInstanceNode.checkNode
import amf.aml.internal.parse.instances.DialectInstanceParser.{computeParsingScheme, emptyElement, typesFrom}
import amf.aml.internal.parse.instances.InstanceNodeIdHandling.generateNodeId
import amf.aml.internal.parse.instances.parser.IncludeNodeParser.resolveLink
import amf.aml.internal.parse.instances.{DialectInstanceContext, NodeMappableHelper}
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.common.validation.AMFStyle
import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.client.scala.validation.AMFValidationResult
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.parser.{Root, YMapOps}
import amf.core.internal.utils.AmfStrings
import amf.validation.internal.shacl.custom.CustomShaclValidator
import org.yaml.model._

case class InstanceNodeParser(root: Root)(implicit ctx: DialectInstanceContext) extends NodeMappableHelper {

  private val map: YMap = root.parsed.asInstanceOf[SyamlParsedDocument].document.as[YMap]

  def parse(path: String, id: String, entry: YNode, mapping: NodeMappable, additionalProperties: Map[String, Any])(
      implicit ctx: DialectInstanceContext): DialectDomainElement =
    parse(path, id, entry, mapping, additionalProperties, givenAnnotations = None)

  def parse(path: String,
            defaultId: String,
            ast: YNode,
            mappable: NodeMappable,
            additionalProperties: Map[String, Any] = Map(),
            rootNode: Boolean = false,
            givenAnnotations: Option[Annotations],
            additionalKey: Option[String] = None,
  )(implicit ctx: DialectInstanceContext): DialectDomainElement = {
    val result: DialectDomainElement = ast.tagType match {
      case YType.Map =>
        val astMap = ast.as[YMap]
        parseNodeMap(path,
                     defaultId,
                     astMap,
                     ast,
                     mappable,
                     additionalProperties,
                     rootNode,
                     givenAnnotations,
                     additionalKey)

      case YType.Str     => resolveLink(ast, mappable, defaultId, givenAnnotations)
      case YType.Include => resolveLink(ast, mappable, defaultId, givenAnnotations)
      case YType.Null    => emptyElement(defaultId, ast, mappable, givenAnnotations)
      case _ =>
        ctx.eh.violation(DialectError, defaultId, "Cannot parse AST node for node in dialect instance", ast.location)
        DialectDomainElement().withId(defaultId)
    }
    // if we are parsing a patch document we mark the node as abstract

    if (ctx.isPatch) result.withAbstract(true)
    result
  }

  private def assignDefinedByAndTypes(mappable: NodeMappable, result: DialectDomainElement) = {
    mappable match {
      case mapping: NodeMapping =>
        result
          .withDefinedBy(mapping)
          .withInstanceTypes(typesFrom(mapping))
      case _ => // ignore
    }
  }

  private def checkNodeForAdditionalKeys(id: String,
                                         nodetype: String,
                                         entries: Map[YNode, YNode],
                                         mapping: NodeMapping,
                                         ast: YPart,
                                         rootNode: Boolean,
                                         additionalKey: Option[String])(implicit ctx: DialectInstanceContext): Unit = {
    checkNode(id, nodetype, entries, mapping, ast, rootNode, additionalKey)
  }

  private def parseNodeMap(path: String,
                           defaultId: String,
                           astMap: YMap,
                           ast: YNode,
                           mappable: NodeMappable,
                           additionalProperties: Map[String, Any],
                           rootNode: Boolean,
                           givenAnnotations: Option[Annotations],
                           additionalKey: Option[String])(implicit ctx: DialectInstanceContext) = {
    computeParsingScheme(astMap) match {
      case "$ref"     => RefNodeParser.parse(defaultId, astMap, mappable, root)
      case "$include" => IncludeNodeParser.parse(ast, mappable, defaultId, givenAnnotations)
      case _ =>
        mappable match {
          case conditional: AnyMapping if conditional.ifMapping.nonEmpty =>
            parseConditionally(path, defaultId, astMap, conditional)
          case mapping: NodeMapping =>
            parseWithNodeMapping(defaultId,
                                 astMap,
                                 ast,
                                 additionalProperties,
                                 rootNode,
                                 givenAnnotations,
                                 additionalKey,
                                 mapping)
          case unionMapping: UnionNodeMapping =>
            parseObjectUnion(defaultId, Seq(path), ast, unionMapping, additionalProperties)
        }

    }
  }

  object IfThenElseBranchCriteria {
    def choose(map: YMap, mapping: AnyMapping)(implicit ctx: DialectInstanceContext): Option[String] = {
      mapping.ifMapping.option()
        .flatMap(ifMappingId => choose(map, ifMappingId, mapping))
    }

    def choose(map: YMap, ifMappingId: String, mapping: AnyMapping)(implicit ctx: DialectInstanceContext): Option[String] = {
      ctx.findNodeMapping(ifMappingId).flatMap { ifMapping =>
        val (ifParsedNode, conformsParsing) = couldParse(map, ifMapping)
        if (!conformsParsing) return mapping.elseMapping.option()
        else {
          val report        = validateParsed(ifMapping, ifParsedNode)
          val isIfCompliant = report.conforms
          if (isIfCompliant) return mapping.thenMapping.option()
          else return mapping.elseMapping.option()
        }
      }
    }
    private def couldParse(map: YMap, ifMapping: ctx.NodeMappable) = {
      val nextContext = ctx.copy(DefaultErrorHandler())
      val ifParsed    = parse("", "if", map, ifMapping, Map.empty)(nextContext)
      val conforms    = ignoreClosedShapeErrors(nextContext.eh.getResults).isEmpty
      (ifParsed, conforms)
    }

    private def ignoreClosedShapeErrors(results: Seq[AMFValidationResult]): Seq[AMFValidationResult] = {
      results.filterNot(_.validationId.contains("closed"))
    }

    private def validateParsed(ifMapping: ctx.NodeMappable, ifParsed: DialectDomainElement) = {
      val mappingsInTree = AmlSubGraphCollector.collect(ifMapping.id, ctx.dialect)
      val validator      = new CustomShaclValidator(Map.empty, AMFStyle)
      val validations = ctx.constraints
        .map(p => p.validations.filter(x => x.targetClass.intersect(mappingsInTree).nonEmpty))
        .getOrElse(Nil)
      val report = validator.validate(ifParsed, validations)
      report
    }
  }

  private def parseConditionally(path: String,
                                 defaultId: String,
                                 astMap: YMap,
                                 mappable: AnyMapping): DialectDomainElement = {
    IfThenElseBranchCriteria
      .choose(astMap, mappable)
      .flatMap(ctx.findNodeMapping)
      .map(mapping => parse(path, defaultId, astMap, mapping, Map.empty)) match {
      case Some(mapping) => mapping
      case None          =>
        // TODO: Violation
        DialectDomainElement().withId(defaultId)
    }
  }

  private def parseWithNodeMapping(defaultId: String,
                                   astMap: YMap,
                                   ast: YNode,
                                   additionalProperties: Map[String, Any],
                                   rootNode: Boolean,
                                   givenAnnotations: Option[Annotations],
                                   additionalKey: Option[String],
                                   mapping: NodeMapping)(implicit ctx: DialectInstanceContext) = {
    val annotations = givenAnnotations.getOrElse(Annotations(ast))
    val node: DialectDomainElement = DialectDomainElement(defaultId.urlDecoded, mapping, annotations)
    val finalId = generateNodeId(node, astMap, Seq(defaultId), defaultId, mapping, additionalProperties, rootNode, root)
    node.withId(finalId)
    parseAnnotations(astMap, node, ctx.declarations)
    mapping.propertiesMapping().foreach { propertyMapping =>
      val propertyName = propertyMapping.name().value()
      astMap.key(propertyName) match {
        case Some(entry) =>
          val nestedId =
            if (Option(ctx.dialect.documents()).flatMap(_.selfEncoded().option()).getOrElse(false) && rootNode)
              defaultId + "#/"
            else defaultId
          parseProperty(nestedId, entry, propertyMapping, node)
        case None => // ignore
      }
    }
    val shouldErrorOnExtraProperties = mapping.closed.option().getOrElse(true) // default behaviour is to error out
    if (shouldErrorOnExtraProperties) checkNodeForAdditionalKeys(finalId, mapping.id, astMap.map, mapping, astMap, rootNode, additionalKey)
    node
  }

  private def parseProperty(id: String,
                            propertyEntry: YMapEntry,
                            property: PropertyMapping,
                            node: DialectDomainElement)(implicit ctx: DialectInstanceContext): Unit = {
    new ElementPropertyParser(root, map, parse).parse(id, propertyEntry, property, node)
  }

  private def parseObjectUnion[T <: DomainElement](
      defaultId: String,
      path: Seq[String],
      ast: YNode,
      unionMapping: NodeWithDiscriminator[_],
      additionalProperties: Map[String, Any] = Map())(implicit ctx: DialectInstanceContext): DialectDomainElement = {

    ObjectUnionParser.parse(defaultId, path, ast, unionMapping, additionalProperties, root, map, parseProperty)
  }
}
