package amf.aml.internal.parse.dialects.nodemapping.like

import amf.aml.client.scala.model.domain.{NodeMapping, PropertyMapping}
import amf.aml.internal.metamodel.domain.{MergePolicies, NodeMappingModel, PropertyMappingModel}
import amf.aml.internal.parse.common.AnnotationsParser.parseAnnotations
import amf.aml.internal.parse.dialects.DialectAstOps.{DialectScalarValueEntryParserOpts, DialectYMapOps}
import amf.aml.internal.parse.dialects.DialectContext
import amf.aml.internal.parse.dialects.property.like.PropertyLikeMappingParser
import amf.aml.internal.parse.instances.BaseDirective
import amf.aml.internal.validate.DialectValidations
import amf.aml.internal.validate.DialectValidations.{DialectError, VariablesDefinedInBase}
import amf.core.client.scala.model.domain.{AmfScalar, DomainElement}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.datanode.DataNodeParser
import amf.core.internal.metamodel.domain.ShapeModel
import amf.core.internal.parser.YNodeLikeOps
import amf.core.internal.parser.domain.SearchScope.All
import amf.core.internal.parser.domain.{Annotations, ScalarNode, SearchScope, ValueNode}
import amf.core.internal.utils.AmfStrings
import amf.core.internal.validation.CoreValidations.SyamlError
import org.mulesoft.common.collections._
import org.yaml.model._

import scala.collection.immutable

trait DefaultFacetParsing {
  protected def parseDefault(map: YMap, element: DomainElement)(implicit ctx: DialectContext): Unit = {
    map.key(
      "default",
      entry => {
        val dataNode = DataNodeParser(entry.value).parse()
        element.set(ShapeModel.Default, dataNode, Annotations(entry))
      }
    )
  }
}

class NodeMappingParser(implicit ctx: DialectContext)
    extends NodeMappingLikeParserInterface
    with DefaultFacetParsing
    with AnyMappingParser {

  override def parse(map: YMap, adopt: DomainElement => Any, isFragment: Boolean): NodeMapping = {

    val nodeMapping = NodeMapping(map)
    adopt(nodeMapping)

    super.parse(map, nodeMapping)

    if (!isFragment) ctx.closedNode("nodeMapping", nodeMapping.id, map)

    map.key(
      "classTerm",
      entry => {
        val value       = ValueNode(entry.value)
        val classTermId = value.string().toString
        ctx.declarations.findClassTerm(classTermId, SearchScope.All) match {
          case Some(classTerm) =>
            nodeMapping.withNodeTypeMapping(classTerm.id)
          case _ =>
            ctx.eh.violation(
              DialectError,
              nodeMapping.id,
              s"Cannot find class term with alias $classTermId",
              entry.value.location
            )
        }
      }
    )

    map.key(
      "additionalProperties",
      entry => {
        val value   = entry.value.as[Boolean]
        val negated = !value
        nodeMapping.set(NodeMappingModel.Closed, AmfScalar(negated, Annotations.inferred()), Annotations(entry))
      }
    )

    map.key(
      "patch",
      entry => {
        val patchMethod = ScalarNode(entry.value).string()
        nodeMapping.set(NodeMappingModel.MergePolicy, patchMethod, Annotations(entry))
        val patchMethodValue = patchMethod.toString
        if (!MergePolicies.isAllowed(patchMethodValue)) {
          ctx.eh.violation(
            DialectError,
            nodeMapping.id,
            s"Unsupported node mapping patch operation '$patchMethod'",
            entry.value.location
          )
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
                .adopted(nodeMapping.id + "/property/" + entry.key.as[YScalar].text.urlComponentEncoded),
            nodeMapping.id
          )
        }
        val (withTerm, withoutTerm) = properties.partition(_.nodePropertyMapping().option().nonEmpty)
        val filterProperties: immutable.Iterable[PropertyMapping] = withTerm
          .filter(_.nodePropertyMapping().option().nonEmpty)
          .legacyGroupBy(p => p.nodePropertyMapping().value())
          .flatMap({
            case (termKey, values) if values.length > 1 =>
              ctx.eh.violation(
                DialectError,
                values.head.id,
                s"Property term value must be unique in a node mapping. Term $termKey repeated",
                values.head.annotations
              )
              values.headOption
            case other => other._2.headOption
          })
        nodeMapping.setArrayWithoutId(
          NodeMappingModel.PropertiesMapping,
          withoutTerm ++ filterProperties.toSeq,
          Annotations(entry)
        )
      }
    )

    map.key(
      "extends",
      entry => {
        val references: Seq[YNode] = entry.value.tagType match {
          case YType.Seq => entry.value.as[YSequence].nodes
          case YType.Str => Seq(entry.value)
          case tagType =>
            ctx.eh.violation(
              SyamlError,
              "",
              s"${YType.Seq} or ${YType.Str} expected in 'extends', found [$tagType]",
              entry.value.location
            )
            Seq.empty
        }
        val parsed = references.map { node: YNode =>
          val reference = node.toOption[YScalar]
          reference match {
            case Some(_) =>
              (
                reference,
                NodeMappingLikeParser.resolveNodeMappingLink(map, node, adopt)
              ) // we don't need to adopt, it is a link
            case _ => (reference, None)
          }
        }
        val resolved: Seq[NodeMapping] = parsed.flatMap {
          case (_, Some(resolvedNodeMapping: NodeMapping)) =>
            Seq(resolvedNodeMapping)
          case (reference, None) =>
            ctx.eh.violation(
              DialectError,
              nodeMapping.id,
              s"Cannot find extended node mapping with reference '${reference.map(_.toString()).getOrElse("")}'",
              entry.value.location
            )
            Nil
        }
        // we need a different Id for each link, adopt makes them equal, but we cannot just reuse the link id because of the
        // mechanism to resolve references, based on the URL of the base document.
        // this is a hack to get unique IDs
        nodeMapping.withExtends(resolved).extend.zipWithIndex.foreach { case (e, i) =>
          e.withId(s"${e.id}-link-extends-${i}")
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
          ctx.eh.warning(
            VariablesDefinedInBase,
            nodeMapping.id,
            s"Base $base contains idTemplate variables overridable by $$base directive",
            entry.value.location
          )
        }
      }
    )

    nodeMapping.idTemplate.option().foreach(validateTemplate(_, map, nodeMapping.propertiesMapping()))

    parseAnnotations(map, nodeMapping, ctx.declarations)

    ctx.declarations.+=(nodeMapping)

    nodeMapping

  }

  private def parsePropertyMapping(entry: YMapEntry, adopt: PropertyMapping => Any, nodeId: String): PropertyMapping = {
    val name = ScalarNode(entry.key).string()
    entry.value.tagType match {
      case YType.Map =>
        val map             = entry.value.as[YMap]
        val propertyMapping = PropertyMapping(entry.value).set(PropertyMappingModel.Name, name, Annotations(entry.key))

        adopt(propertyMapping)
        ctx.closedNode("propertyMapping", propertyMapping.id, map)

        PropertyLikeMappingParser(map, propertyMapping).parse()

        parseMapKey(map, propertyMapping)
        parseMapValue(map, propertyMapping)
        parsePatch(map, propertyMapping)
        parseAnnotations(map, propertyMapping, ctx.declarations)
        parseDefault(map, propertyMapping)
        propertyMapping
      case _ =>
        val p = PropertyMapping(Annotations(entry)).set(PropertyMappingModel.Name, name, Annotations(entry.key))
        ctx.eh.violation(
          DialectValidations.PropertyMappingMustBeAMap,
          nodeId,
          s"Property mapping $name must be a map",
          entry.location
        )
        p
    }
  }

  private def parsePatch(map: YMap, propertyMapping: PropertyMapping): Unit = {
    map.key(
      "patch",
      entry => {
        val patchMethod = ScalarNode(entry.value).string()
        propertyMapping.set(PropertyMappingModel.MergePolicy, patchMethod, Annotations(entry))
        val patchMethodValue = patchMethod.toString
        if (!MergePolicies.isAllowed(patchMethodValue)) {
          ctx.eh.violation(
            DialectError,
            propertyMapping.id,
            s"Unsupported property mapping patch operation '$patchMethod'",
            entry.value.location
          )
        }
      }
    )
  }

  private def parseMapKey(map: YMap, propertyMapping: PropertyMapping): Unit = {
    val mapKey     = map.key("mapKey")
    val mapTermKey = map.key("mapTermKey")

    for {
      _ <- mapKey
      _ <- mapTermKey
    } yield {
      ctx.eh.violation(DialectError, propertyMapping.id, s"mapKey and mapTermKey are mutually exclusive", map.location)
    }

    mapTermKey.fold({
      mapKey.foreach(entry => {
        val propertyLabel = ValueNode(entry.value).string().toString
        propertyMapping.withMapKeyProperty(propertyLabel, Annotations(entry.value))
      })
    })(entry => {
      val propertyTermId = ValueNode(entry.value).string().toString
      getTermIfValid(propertyTermId, propertyMapping.id, entry.value).foreach { p =>
        propertyMapping.withMapTermKeyProperty(p, Annotations(entry.value))
      }
    })
  }

  private def parseMapValue(map: YMap, propertyMapping: PropertyMapping): Unit = {
    val mapValue     = map.key("mapValue")
    val mapTermValue = map.key("mapTermValue")

    for {
      _ <- mapValue
      _ <- mapTermValue
    } yield {
      ctx.eh
        .violation(DialectError, propertyMapping.id, s"mapValue and mapTermValue are mutually exclusive", map.location)
    }

    mapTermValue.fold({
      mapValue.foreach(entry => {
        val propertyLabel = ValueNode(entry.value).string().toString
        propertyMapping.withMapValueProperty(propertyLabel, Annotations(entry.value))
      })
    })(entry => {
      val propertyTermId = ValueNode(entry.value).string().toString
      getTermIfValid(propertyTermId, propertyMapping.id, entry.value).foreach { p =>
        propertyMapping.withMapTermValueProperty(p, Annotations(entry.value))
      }
    })

  }

  private def getTermIfValid(iri: String, propertyMappingId: String, ast: YPart): Option[String] = {
    Namespace(iri).base match {
      case Namespace.Data.base => Some(iri)
      case _ =>
        ctx.declarations.findPropertyTerm(iri, All) match {
          case Some(term) => Some(term.id)
          case _ =>
            ctx.eh
              .violation(DialectError, propertyMappingId, s"Cannot find property term with alias $iri", ast.location)
            None
        }
    }
  }

  def validateTemplate(template: String, map: YMap, propMappings: Seq[PropertyMapping]): Unit = {
    getVariablesFromTemplate(template).foreach { variable =>
      propMappings.find(_.name().value() == variable) match {
        case Some(prop) if !prop.isMandatory =>
          ctx.eh.violation(
            DialectError,
            prop.id,
            s"PropertyMapping for idTemplate variable '$variable' must be mandatory",
            map.location
          )
        case None =>
          ctx.eh
            .violation(DialectError, "", s"Missing propertyMapping for idTemplate variable '$variable'", map.location)
        case _ => // ignore
      }
    }

    def getVariablesFromTemplate(template: String): Iterator[String] = {
      val regex = "(\\{[^}]+\\})".r
      regex.findAllIn(template).map { varMatch =>
        varMatch.replace("{", "").replace("}", "")
      }
    }
  }

}

object NodeMappingParser {
  def apply()(implicit ctx: DialectContext): NodeMappingParser = new NodeMappingParser
}
