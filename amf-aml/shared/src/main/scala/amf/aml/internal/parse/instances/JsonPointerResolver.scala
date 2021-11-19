package amf.aml.internal.parse.instances

import amf.core.internal.parser.domain.Annotations
import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMappable}
import org.yaml.model.YMap
import amf.core.internal.parser.domain.BaseSpecParser
import amf.core.internal.parser.{Root, YMapOps}
import amf.core.internal.unsafe.PlatformSecrets

/*
 * TODO: should be a class which is passed as parameter to the dialect instance parser. Most of all because of the resolvedPath(String) and basePath(String) methods.
 */
object JsonPointerResolver extends NodeMappableHelper with PlatformSecrets {

  def resolveJSONPointer(map: YMap, mapping: NodeMappable, id: String, root: Root)(
      implicit ctx: DialectInstanceContext): DialectDomainElement = {
    val mappingIds = allNodeMappingIds(mapping)
    val entry      = map.key("$ref").get
    val pointer    = entry.value.as[String]
    val fullPointer = if (pointer.startsWith("#")) {
      root.location + pointer
    } else {
      resolvedPath(pointer, root)
    }

    ctx.findJsonPointer(fullPointer) map { node =>
      if (mappingIds.contains(node.definedBy.id)) {
        node
          .link(pointer, Annotations(map))
          .asInstanceOf[DialectDomainElement]
          .withId(id)
          .withInstanceTypes(Seq(mapping.id))
      } else {
        val linkedNode = DialectDomainElement(map).withId(id).withInstanceTypes(Seq(mapping.id))
        linkedNode.unresolved(fullPointer, Nil, Some(map.location))
        linkedNode
      }
    } getOrElse {
      val linkedNode = DialectDomainElement(map).withId(id).withInstanceTypes(Seq(mapping.id))
      linkedNode.unresolved(fullPointer, Nil, Some(map.location))
      linkedNode
    }
  }

  private def resolvedPath(str: String, root: Root): String = {
    val base = root.location
    val fullPath =
      if (str.startsWith("/")) str
      else if (str.contains("://")) str
      else if (str.startsWith("#")) base.split("#").head + str
      else basePath(base) + str
    if (fullPath.contains("#")) {
      val parts = fullPath.split("#")
      platform.resolvePath(parts.head) + "#" + parts.last
    } else {
      platform.resolvePath(fullPath)
    }
  }

  private def basePath(path: String): String = {
    val withoutHash = if (path.contains("#")) path.split("#").head else path
    withoutHash.splitAt(withoutHash.lastIndexOf("/"))._1 + "/"
  }
}
