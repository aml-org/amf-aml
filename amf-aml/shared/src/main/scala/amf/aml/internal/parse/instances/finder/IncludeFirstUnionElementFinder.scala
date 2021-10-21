package amf.aml.internal.parse.instances.finder

import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping}
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import org.yaml.model.{YMap, YNode, YScalar}

object IncludeFirstUnionElementFinder {

  def find(ast: YNode, allPossibleMappings: Seq[NodeMapping], id: String, root: YMap)(
      implicit ctx: DialectInstanceContext): DialectDomainElement = {
    val refTuple = ctx.link(ast) match {
      case Left(key) =>
        (key,
         allPossibleMappings
           .map(mapping => ctx.declarations.findDialectDomainElement(key, mapping, SearchScope.Fragments))
           .collectFirst { case Some(x) => x })
      case _ =>
        val text = ast.as[YScalar].text
        (text,
         allPossibleMappings
           .map(mapping => ctx.declarations.findDialectDomainElement(text, mapping, SearchScope.Named))
           .collectFirst { case Some(x) => x })
    }
    refTuple match {
      case (text: String, Some(s)) =>
        val linkedNode = s
          .link(text, Annotations(ast.value))
          .asInstanceOf[DialectDomainElement]
          .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
        linkedNode
      case (text: String, _) =>
        val linkedNode = DialectDomainElement(root).withId(id)
        linkedNode.unresolved(text, Nil, Some(root.location))
        linkedNode
    }
  }
}
