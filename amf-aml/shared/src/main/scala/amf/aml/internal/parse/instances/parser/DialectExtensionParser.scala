package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.parse.instances.DialectInstanceParser.{computeParsingScheme, pathSegment}
import amf.aml.internal.parse.instances.parser.ObjectCollectionPropertyParser.NodeParser
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.internal.parser.{Root, YMapOps}
import org.yaml.model.{YMap, YMapEntry, YScalar, YType}

object DialectExtensionParser {

  def parse(id: String,
            propertyEntry: YMapEntry,
            property: PropertyLikeMapping[_],
            node: DialectDomainElement,
            root: Root,
            nodeParser: NodeParser)(implicit ctx: DialectInstanceContext): Unit = {
    val nestedObjectId = pathSegment(id, List(propertyEntry.key.as[YScalar].text))
    propertyEntry.value.tagType match {
      case YType.Str | YType.Include =>
        resolveLinkProperty(propertyEntry, property, nestedObjectId, node)
      case YType.Map =>
        val map = propertyEntry.value.as[YMap]
        map.key("$dialect") match {
          case Some(nested) if nested.value.tagType == YType.Str =>
            val dialectNode = nested.value.as[YScalar].text
            // TODO: resolve dialect node URI to absolute normalised URI
            ctx.nodeMappableFinder.findNode(dialectNode) match {
              case Some((dialect, nodeMapping)) =>
                ctx.nestedDialects ++= Seq(dialect)
                ctx.withCurrentDialect(dialect) {
                  val dialectDomainElement =
                    nodeParser(id, nestedObjectId, propertyEntry.value, nodeMapping, Map.empty, false)
                  node.withObjectField(property, dialectDomainElement, Right(propertyEntry))
                }
              case None =>
                ctx.eh.violation(DialectError,
                                 id,
                                 s"Cannot find dialect for nested anyNode mapping $dialectNode",
                                 nested.value.location)
            }
          case None =>
            computeParsingScheme(map) match {
              case "$include" =>
                val includeEntry = map.key("$include").get
                resolveLinkProperty(includeEntry, property, nestedObjectId, node, isRef = true)
              case "$ref" =>
                resolveJSONPointerProperty(map, property, nestedObjectId, node, root)
              case _ =>
                ctx.eh.violation(DialectError, id, "$dialect key without string value or link", map.location)
            }
        }
    }
  }

  protected def resolveLinkProperty(propertyEntry: YMapEntry,
                                    mapping: PropertyLikeMapping[_],
                                    id: String,
                                    node: DialectDomainElement,
                                    isRef: Boolean = false)(implicit ctx: DialectInstanceContext): Unit =
    LinkIncludePropertyParser.parse(propertyEntry, mapping, id, node, isRef)

  protected def resolveJSONPointerProperty(map: YMap,
                                           mapping: PropertyLikeMapping[_],
                                           id: String,
                                           node: DialectDomainElement,
                                           root: Root)(implicit ctx: DialectInstanceContext): Unit =
    JSONPointerPropertyParser.parse(map, mapping, id, node, root)
}
