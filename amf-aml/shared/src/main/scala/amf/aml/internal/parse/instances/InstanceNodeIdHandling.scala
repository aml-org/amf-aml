package amf.aml.internal.parse.instances

import amf.aml.internal.annotations.CustomId
import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping}
import amf.aml.internal.parse.instances.parser.ExternalLinkGenerator.overrideBase
import amf.aml.internal.validate.DialectValidations.DialectError
import org.yaml.model.{YMap, YMapEntry, YNode, YScalar, YType}
import amf.core.internal.parser.{Root, YMapOps}
import amf.core.internal.utils.AmfStrings

import scala.collection.mutable

case class Ctx(map: YMap, nodeMapping: NodeMapping, id: String)

object InstanceNodeIdHandling {
  def explicitNodeId(node: Option[DialectDomainElement], nodeMap: YMap, ctx: DialectInstanceContext): String = {
    // explicit $id
    val entry = nodeMap.key("$id").get
    val rawId = entry.value.as[YScalar].text
    val externalId = if (rawId.contains("://")) {
      rawId
    } else {
      (ctx.dialect.location().getOrElse(ctx.dialect.id).split("#").head + s"#$rawId").replace("##", "#")
    }
    node.foreach((n) => n.annotations += CustomId())
    externalId
  }

  def idTemplate(node: DialectDomainElement, nodeMap: YMap, path: Seq[String], mapping: NodeMapping, root: Root)(
      implicit ctx: DialectInstanceContext): String = {
    val template = replaceTemplateVariables(node.id, nodeMap, mapping.idTemplate.value())
    prependRootIfIsRelative(template, path, root)
  }

  private def prependRootIfIsRelative(template: String, path: Seq[String], root: Root): String = {
    val templateRoot = root.location
    if (template.contains("://"))
      template
    else if (template.startsWith("/"))
      templateRoot + "#" + template
    else if (template.startsWith("#"))
      templateRoot + template
    else {
      val pathLocation = (path ++ template.split("/")).mkString("/")
      if (pathLocation.startsWith(templateRoot) || pathLocation.contains("#")) {
        pathLocation
      } else {
        templateRoot + "#" + pathLocation
      }
    }
  }

  private def replaceTemplateVariables(nodeId: String, nodeMap: YMap, originalTemplate: String)(
      implicit ctx: DialectInstanceContext): String = {
    var template = originalTemplate
    // template resolution
    val regex = "(\\{[^}]+\\})".r
    regex.findAllIn(template).foreach { varMatch =>
      val variable = varMatch.replace("{", "").replace("}", "")
      nodeMap.key(variable) match {
        case Some(entry) =>
          val value = entry.value.tagType match {
            case YType.Str => entry.value.as[String]
            case _         => entry.value.value.toString
          }
          template = template.replace(varMatch, value)
        case None =>
          ctx.eh.violation(DialectError, nodeId, s"Missing ID template variable '$variable' in node", nodeMap.location)
      }
    }
    template
  }

  def generateNodeId(node: DialectDomainElement,
                     nodeMap: YMap,
                     path: Seq[String],
                     encodedDefaultId: String,
                     mapping: NodeMapping,
                     additionalProperties: Map[String, Any] = Map(),
                     rootNode: Boolean,
                     root: Root)(implicit ctx: DialectInstanceContext): String = {
    val defaultDecodedId = encodedDefaultId.urlDecoded
    val generatedId =
      if (rootNode && isSelfEncoded)
        defaultDecodedId // if this is self-encoded just reuse the dialectId computed and don't try to generate a different identifier
      else {
        if (nodeMap.key("$id").isDefined) {
          explicitNodeId(Some(node), nodeMap, ctx)
        } else if (mapping.idTemplate.nonEmpty) {
          idTemplate(node, nodeMap, path, mapping, root)
        } else if (mapping.primaryKey().nonEmpty) {
          primaryKeyNodeId(node, nodeMap, path, defaultDecodedId, mapping, additionalProperties)
        } else {
          defaultDecodedId
        }
      }
    overrideBase(generatedId, nodeMap).urlEncoded
  }

  private def primaryKeyNodeId(
      node: DialectDomainElement,
      nodeMap: YMap,
      path: Seq[String],
      defaultId: String,
      mapping: NodeMapping,
      additionalProperties: Map[String, Any] = Map())(implicit ctx: DialectInstanceContext): String = {
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
              ctx.eh
                .violation(DialectError,
                           node.id,
                           s"Cannot find unique mandatory property '$propertyName'",
                           nodeMap.location)
              allFound = false
          }
      }
    }
    if (allFound) { path.mkString("/") + "/" + keyId.mkString("_") } else { defaultId }
  }

  private def isSelfEncoded(implicit ctx: DialectInstanceContext) = {
    Option(ctx.dialect.documents()).flatMap(_.selfEncoded().option()).getOrElse(false)
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
}

trait InstanceNodeIdHandling extends BaseDirectiveOverride { this: DialectInstanceParser =>

  protected def generateNodeId(node: DialectDomainElement,
                               nodeMap: YMap,
                               path: Seq[String],
                               defaultId: String,
                               mapping: NodeMapping,
                               additionalProperties: Map[String, Any] = Map(),
                               rootNode: Boolean): String = {
    InstanceNodeIdHandling.generateNodeId(node,
                                          nodeMap,
                                          path,
                                          defaultId,
                                          mapping,
                                          additionalProperties,
                                          rootNode,
                                          root)
  }

  protected def idTemplate(node: DialectDomainElement,
                           nodeMap: YMap,
                           path: Seq[String],
                           mapping: NodeMapping): String = {
    InstanceNodeIdHandling.idTemplate(node, nodeMap, path, mapping, root)
  }

  protected def explicitNodeId(node: Option[DialectDomainElement], nodeMap: YMap): String = {
    InstanceNodeIdHandling.explicitNodeId(node, nodeMap, ctx)
  }
}
