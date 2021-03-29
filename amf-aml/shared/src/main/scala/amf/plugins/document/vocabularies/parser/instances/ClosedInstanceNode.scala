package amf.plugins.document.vocabularies.parser.instances

import amf.plugins.document.vocabularies.model.domain.NodeMapping
import org.yaml.model.{YNode, YPart, YScalar}

object ClosedInstanceNode {

  def checkRootNode(id: String,
                    nodetype: String,
                    entries: Map[YNode, YNode],
                    mapping: NodeMapping,
                    ast: YPart,
                    additionalKey: Option[String])(implicit ctx: DialectInstanceContext): Unit = {
    val excluded = ctx.rootProps
    checkNode(id, nodetype, entries, mapping, ast, excluded ++ additionalKey.toSet)
  }

  def checkClosedNode(id: String,
                      nodetype: String,
                      entries: Map[YNode, YNode],
                      mapping: NodeMapping,
                      ast: YPart,
                      additionalKey: Option[String])(implicit ctx: DialectInstanceContext): Unit = {
    checkNode(id, nodetype, entries, mapping, ast, additionalKey.toSet)
  }

  private def checkNode(id: String,
                        nodetype: String,
                        entries: Map[YNode, YNode],
                        mapping: NodeMapping,
                        ast: YPart,
                        excludedKeys: Set[String])(implicit ctx: DialectInstanceContext): Unit = {
    val allowedEntryKeys = mapping.propertiesMapping().map(_.name().value()).toSet.union(excludedKeys)
    val entriesToTest = entries.keys
      .map(_.value.asInstanceOf[YScalar].text)
      .filter(p => !isInstanceKeyword(p) && !isAnnotation(p))
      .toSet
    val outside = entriesToTest.diff(allowedEntryKeys)
    outside.foreach { prop =>
      val posAst = entries.find(_._1.toString == prop).map(_._2).getOrElse(ast)
      ctx.closedNodeViolation(id, prop, nodetype, posAst)
    }
  }

  private def isAnnotation(p: String) = p.startsWith("(") || p.startsWith("x-")

  private def isInstanceKeyword(p: String) = p.startsWith("$")
}
