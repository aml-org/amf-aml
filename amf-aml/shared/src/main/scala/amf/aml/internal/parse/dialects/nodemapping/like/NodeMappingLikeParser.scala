package amf.aml.internal.parse.dialects.nodemapping.like

import amf.aml.client.scala.model.domain.NodeMappable.AnyNodeMappable
import amf.aml.client.scala.model.domain.NodeMapping
import amf.aml.internal.parse.dialects.DialectContext
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import org.yaml.model.{YMap, YMapEntry, YNode, YScalar}

object NodeMappingLikeParser {

  def parse(entry: YMapEntry, adopt: DomainElement => Any, isFragment: Boolean = false)(implicit
      ctx: DialectContext
  ): AnyNodeMappable = {

    entry.value.as[YMap] match {
      case uMap if applies(uMap, UnionNodeMappingParser.identifierKey) =>
        UnionNodeMappingParser().parse(uMap, adopt, isFragment)
      case nMap => NodeMappingParser().parse(nMap, adopt, isFragment)
    }

  }

  def resolveNodeMappingLink(map: YMap, entry: YNode, adopt: DomainElement => Any)(implicit
      ctx: DialectContext
  ): Some[NodeMapping] = {
    val refTuple = ctx.link(entry) match {
      case Left(key) =>
        (key, ctx.declarations.findNodeMapping(key, SearchScope.Fragments))
      case _ =>
        val text = entry.as[YScalar].text
        (text, ctx.declarations.findNodeMapping(text, SearchScope.Named))
    }
    refTuple match {
      case (text: String, Some(s)) =>
        val linkedNode = s
          .link(text, Annotations(entry.value))
          .asInstanceOf[NodeMapping]
        adopt(
          linkedNode
        ) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
        Some(linkedNode)
      case (text: String, _) =>
        val linkedNode = NodeMapping(map)
        adopt(linkedNode)
        linkedNode.unresolved(text, Nil, Some(map.location))
        Some(linkedNode)
    }
  }

  private def applies(map: YMap, key: String): Boolean = map.key(key).isDefined

}
