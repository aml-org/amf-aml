package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.DialectDomainElement
import amf.aml.internal.annotations.RefInclude
import amf.aml.internal.parse.instances.{DialectInstanceContext, NodeMappableHelper}
import amf.core.internal.annotations.SourceYPart
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import org.yaml.model.{YNode, YScalar}

object IncludeNodeParser extends NodeMappableHelper {

  def parse(ast: YNode, mapping: NodeMappable, id: String, givenAnnotations: Option[Annotations])(implicit
      ctx: DialectInstanceContext
  ): DialectDomainElement = {
    val link = resolveLink(ast, mapping, id, givenAnnotations)
    link.annotations += RefInclude()
    link
  }

  def resolveLink(ast: YNode, mapping: NodeMappable, id: String, givenAnnotations: Option[Annotations])(implicit
      ctx: DialectInstanceContext
  ): DialectDomainElement = {
    val refTuple = ctx.link(ast) match {
      case Left(key) =>
        (key, ctx.declarations.findDialectDomainElement(key, mapping, SearchScope.Fragments))
      case _ =>
        val text = ast.as[YScalar].text
        (text, ctx.declarations.findDialectDomainElement(text, mapping, SearchScope.Named))
    }
    refTuple match {
      case (text: String, Some(s)) =>
        val linkedNode = s
          .link(text, givenAnnotations.getOrElse(Annotations(ast)))
          .asInstanceOf[DialectDomainElement]
          .withInstanceTypes(Seq(mapping.id))
          .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
        linkedNode
      case (text: String, _) =>
        val loc = givenAnnotations.flatMap(_.find(classOf[SourceYPart])).map(_.ast) match {
          case Some(n) => n.location
          case _       => ast.location
        }
        val linkedNode = DialectDomainElement(givenAnnotations.getOrElse(Annotations(ast)))
          .withId(id)
          .withInstanceTypes(Seq(mapping.id))
        linkedNode.unresolved(text, Nil, Some(loc))
        linkedNode
    }
  }
}
