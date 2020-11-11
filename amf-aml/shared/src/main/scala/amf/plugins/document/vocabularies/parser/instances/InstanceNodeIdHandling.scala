package amf.plugins.document.vocabularies.parser.instances

import amf.plugins.document.vocabularies.annotations.CustomId
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, NodeMapping}
import amf.validation.DialectValidations.DialectError
import org.yaml.model.{YMap, YMapEntry, YNode, YScalar, YType}
import amf.core.parser.YMapOps
import amf.core.utils.AmfStrings

import scala.collection.mutable

case class Ctx(map: YMap, nodeMapping: NodeMapping, id: String)

trait InstanceNodeIdHandling extends BaseIdHanding { this: DialectInstanceParser =>

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
    val generatedId = if (rootNode && Option(ctx.dialect.documents()).flatMap(_.selfEncoded().option()).getOrElse(false))
      defaultId // if this is self-encoded just reuse the dialectId computed and don't try to generate a different identifier
    else {
      if (nodeMap.key("$id").isDefined) {
        explicitNodeId(node, nodeMap, path, defaultId, mapping)
      }
      else if (mapping.idTemplate.nonEmpty) {
        templateNodeId(node, nodeMap, path, defaultId, mapping)
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
    }
    else {
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
      val variable = varMatch.replace("{", "").replace("}", "")
      nodeMap.key(variable) match {
        case Some(entry) =>
          val value = entry.value.value.toString
          template = template.replace(varMatch, value)
        case None =>
          ctx.eh.violation(DialectError, node.id, s"Missing ID template variable '$variable' in node", nodeMap)
      }
    }
    if (template.contains("://"))
      template
    else if (template.startsWith("/"))
      root.location + "#" + template
    else if (template.startsWith("#"))
      root.location + template
    else {
      val pathLocation = (path ++ template.split("/")).mkString("/")
      if (pathLocation.startsWith(root.location) || pathLocation.contains("#")) {
        pathLocation
      }
      else {
        root.location + "#" + pathLocation
      }
    }
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
