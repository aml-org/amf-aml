package amf.plugins.document.vocabularies.plugin.headers

import amf.core.Root
import amf.plugins.document.vocabularies.DialectsRegistry
import org.mulesoft.common.core._

object DialectHeader extends RamlHeaderExtractor with JsonHeaderExtractor with KeyPropertyHeaderExtractor {

  /** Fetch header or dialect directive. */
  def dialectHeaderDirective(document: Root): Option[String] = {
    comment(document).orElse(dialect(document).map(metaText => s"%$metaText")).map(_.stripSpaces)
  }

  /**
    * Extract the root part from a dialect header directive
    * e.g.
    *   Header directive                         Root part
    *   #%Library / Dialect 1.0               -> Library
    *   #%My Fragment / My Test Dialect 1.0   -> My Fragment
    */
  def dialectHeaderDirectiveRootPart(document: Root): Option[String] = {
    for {
      header <- dialectHeaderDirective(document)
    } yield {
      header.substring(1, header.indexOf("/"))
    }
  }

  def apply(root: Root, registry: DialectsRegistry): Boolean = {
    val text = dialectHeaderDirective(root)

    if (isExternal(text)) true
    else {
      val header = text.map(h => h.split("\\|").head)

      header match {
        case Some(ExtensionHeader.DialectHeader) | Some(ExtensionHeader.DialectFragmentHeader) | Some(
                ExtensionHeader.DialectLibraryHeader) | Some(ExtensionHeader.VocabularyHeader) =>
          true
        case Some(other) => registry.findDialectForHeader(other).isDefined
        case _           => dialectInKey(root, registry).isDefined
      }
    }
  }

  private def isExternal(header: Option[String]) = header.exists(h => h.endsWith(">") && h.contains("|<"))
}
