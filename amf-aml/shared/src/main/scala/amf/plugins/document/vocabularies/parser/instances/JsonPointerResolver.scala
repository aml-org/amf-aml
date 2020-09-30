package amf.plugins.document.vocabularies.parser.instances

import amf.core.Root
import amf.core.parser.Annotations
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, NodeMappable}
import org.yaml.model.YMap
import amf.core.parser._
import amf.core.unsafe.PlatformSecrets

/*
 * TODO: should be a class which is passed as parameter to the dialect instance parser. Most of all because of the resolvedPath(String) and basePath(String) methods.
 */
trait JsonPointerResolver extends NodeMappableHelper with PlatformSecrets with BaseSpecParser {

  val root: Root
  implicit val ctx: DialectInstanceContext

  protected def resolveJSONPointer(map: YMap, mapping: NodeMappable, id: String): Option[DialectDomainElement] = {
    val mappingIds = allNodeMappingIds(mapping)
    val entry      = map.key("$ref").get
    val pointer    = entry.value.as[String]
    val fullPointer = if (pointer.startsWith("#")) {
      root.location + pointer
    } else {
      resolvedPath(pointer)
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
        linkedNode.unresolved(fullPointer, map)
        linkedNode
      }
    } orElse {
      val linkedNode = DialectDomainElement(map).withId(id).withInstanceTypes(Seq(mapping.id))
      linkedNode.unresolved(fullPointer, map)
      Some(linkedNode)
    }
  }

  protected def resolvedPath(str: String): String = {
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

  protected def basePath(path: String): String = {
    val withoutHash = if (path.contains("#")) path.split("#").head else path
    withoutHash.splitAt(withoutHash.lastIndexOf("/"))._1 + "/"
  }
}
