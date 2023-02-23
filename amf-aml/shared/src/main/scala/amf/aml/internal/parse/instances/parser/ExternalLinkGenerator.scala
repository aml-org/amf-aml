package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping, PropertyMapping}
import amf.aml.internal.annotations.{CustomBase, CustomId}
import amf.aml.internal.parse.instances.{BaseDirectiveOverride, DialectInstanceContext, InstanceNodeIdHandling}
import amf.aml.internal.parse.instances.DialectInstanceParser.typesFrom
import amf.aml.internal.parse.instances.InstanceNodeIdHandling.idTemplate
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.internal.parser.{Root, YMapOps}
import amf.core.internal.parser.domain.Annotations
import org.yaml.model.{YMap, YMapEntry, YNode, YType}

object ExternalLinkGenerator extends BaseDirectiveOverride {

  type PropertyParser = (String, YMapEntry, PropertyMapping, DialectDomainElement) => Unit

  def generate(id: String, node: YNode, mapping: NodeMapping, root: Root, propertyParser: PropertyParser)(implicit
      ctx: DialectInstanceContext
  ): Option[DialectDomainElement] = {
    lazy val instanceTypes = typesFrom(mapping)
    node.tagType match {
      case YType.Str => // plain link -> we generate an anonymous node and set the id to the ref and correct type information
        val elem = DialectDomainElement()
          .withDefinedBy(mapping)
          .withId(node)
          .withIsExternalLink(true)
          .withInstanceTypes(instanceTypes)
        Some(elem)

      case YType.Map
          if node
            .as[YMap]
            .key("$id")
            .isDefined => // simple link in a reference map
        val refMap = node.as[YMap]

        val id      = InstanceNodeIdHandling.explicitNodeId(None, refMap, ctx)
        val finalId = overrideBase(id, refMap)

        val elem = DialectDomainElement().withDefinedBy(mapping).withId(finalId).withIsExternalLink(true)
        elem.withInstanceTypes(instanceTypes)
        elem.annotations += CustomId()
        refMap.key("$base") match {
          case Some(baseEntry) =>
            elem.annotations += CustomBase(baseEntry.value.toString)
          case _ => // Nothing
        }
        Some(elem)

      case YType.Map if mapping.idTemplate.nonEmpty => // complex reference with mandatory idTemplate
        val refMap = node.as[YMap]

        val element = DialectDomainElement(Annotations(refMap))
        val id      = idTemplate(element, refMap, Nil, mapping, root)
        val finalId = overrideBase(id, refMap)

        // Now we actually parse the provided properties for the node
        val linkReference: DialectDomainElement =
          element
            .withDefinedBy(mapping)
            .withId(finalId)
            .withInstanceTypes(instanceTypes)
            .withIsExternalLink(true) // this is a linkReference

        refMap.key("$base") match {
          case Some(baseEntry) =>
            linkReference.annotations += CustomBase(baseEntry.value.toString)
          case _ => // Nothing
        }

        // TODO why do we parse properties?
        mapping.propertiesMapping().foreach { propertyMapping =>
          val propertyName = propertyMapping.name().value()
          refMap.key(propertyName) match {
            case Some(entry) =>
              propertyParser(finalId, entry, propertyMapping, linkReference)
            case None => // ignore
          }
        }

        // return the parsed reference
        Some(linkReference)

      case _ => // error
        ctx.eh.violation(
          DialectError,
          id,
          "AML links must URI links (strings or maps with $id directive) or ID Template links (maps with idTemplate variables)"
        )
        None
    }
  }
}
