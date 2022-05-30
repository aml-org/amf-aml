package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.annotations.RefInclude
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import org.yaml.model.{YMapEntry, YScalar}

object LinkIncludePropertyParser {
  def parse(
      propertyEntry: YMapEntry,
      mapping: PropertyLikeMapping[_],
      id: String,
      node: DialectDomainElement,
      isRef: Boolean = false
  )(implicit ctx: DialectInstanceContext): Unit = {
    val refTuple = ctx.link(propertyEntry.value) match {
      case Left(key) =>
        (key, ctx.declarations.findAnyDialectDomainElement(key, SearchScope.Fragments))
      case _ =>
        val text = propertyEntry.value.as[YScalar].text
        (text, ctx.declarations.findAnyDialectDomainElement(text, SearchScope.All))
    }
    refTuple match {
      case (text: String, Some(s)) =>
        ctx.nodeMappableFinder.findNode(s.definedBy.id) match {
          case Some((dialect, _)) =>
            ctx.nestedDialects ++= Seq(dialect)
            val linkedExternal = s
              .link(text, Annotations(propertyEntry.value))
              .asInstanceOf[DialectDomainElement]
              .withId(
                  id
              ) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            if (isRef) linkedExternal.annotations += RefInclude()
            node.withObjectField(mapping, linkedExternal, Right(propertyEntry))
          case None =>
            ctx.eh.violation(
                DialectError,
                id,
                s"Cannot find dialect for anyNode node mapping ${s.definedBy.id}",
                propertyEntry.value.location
            )
        }
      case _ =>
        ctx.eh.violation(
            DialectError,
            id,
            s"anyNode reference must be to a known node or an external fragment, unknown value: '${propertyEntry.value}'",
            propertyEntry.value.location
        )
    }
  }
}
