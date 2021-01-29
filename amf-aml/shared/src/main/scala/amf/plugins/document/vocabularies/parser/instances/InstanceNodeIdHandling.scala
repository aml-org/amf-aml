package amf.plugins.document.vocabularies.parser.instances

import amf.plugins.document.vocabularies.annotations.CustomId
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, NodeMapping}
import amf.validation.DialectValidations.DialectError
import org.yaml.model.{YMap, YMapEntry, YNode, YScalar, YType}
import amf.core.parser.YMapOps
import amf.core.utils.AmfStrings

import scala.collection.mutable

case class Ctx(map: YMap, nodeMapping: NodeMapping, id: String)

trait InstanceNodeIdHandling extends BaseDirectiveOverride { this: DialectInstanceParser =>

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
    val generatedId = if (rootNode && isSelfEncoded)
      defaultId // if this is self-encoded just reuse the dialectId computed and don't try to generate a different identifier
    else {
      if (nodeMap.key("$id").isDefined) {
        explicitNodeId(Some(node), nodeMap, path, defaultId, mapping)
      }
      else if (mapping.idTemplate.nonEmpty) {
        idTemplate(node, nodeMap, path, mapping)
      }
      else if (mapping.primaryKey().nonEmpty) {
        primaryKeyNodeId(node, nodeMap, path, defaultId, mapping, additionalProperties)
      }
      else {
        defaultId
      }
    }
    overrideBase(generatedId, nodeMap)
  }


  private def isSelfEncoded = {
    Option(ctx.dialect.documents()).flatMap(_.selfEncoded().option()).getOrElse(false)
  }

  protected def idTemplate(node: DialectDomainElement, nodeMap: YMap, path: Seq[String], mapping: NodeMapping): String = {
    val template = replaceTemplateVariables(node.id, nodeMap, mapping.idTemplate.value())
    prependRootIfIsRelative(template, path)
  }

  protected def prependRootIfIsRelative(template: String, path: Seq[String]): String = {
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
      }
      else {
        templateRoot + "#" + pathLocation
      }
    }
  }

  protected def explicitNodeId(node: Option[DialectDomainElement],
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
    node.foreach((n) => n.annotations += CustomId())
    externalId
  }

  protected def replaceTemplateVariables(nodeId: String, nodeMap: YMap, originalTemplate: String): String = {
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
          ctx.eh.violation(DialectError, nodeId, s"Missing ID template variable '$variable' in node", nodeMap)
      }
    }
    template
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
