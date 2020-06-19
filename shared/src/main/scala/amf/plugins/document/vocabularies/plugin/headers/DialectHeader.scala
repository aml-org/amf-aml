package amf.plugins.document.vocabularies.plugin.headers

import amf.core.Root
import amf.plugins.document.vocabularies.AMLPlugin
import org.mulesoft.common.core._
object DialectHeader extends RamlHeaderExtractor with JsonHeaderExtractor with KeyPropertyHeaderExtractor {
  /** Fetch header or dialect directive. */
  def dialectHeaderDirective(document: Root): Option[String] = {
    comment(document).orElse(dialect(document).map(metaText => s"%$metaText")).map(_.stripSpaces)
  }

  def apply(root: Root): Boolean = {
    val text = dialectHeaderDirective(root)

    if (isExternal(text)) true
    else {
      val header = text.map(h => h.split("\\|").head)

      header match {
        case Some(ExtensionHeader.DialectHeader) | Some(ExtensionHeader.DialectFragmentHeader) | Some(
        ExtensionHeader.DialectLibraryHeader) | Some(ExtensionHeader.VocabularyHeader) =>
          true
        case Some(other) => AMLPlugin.registry.findDialectForHeader(other).isDefined
        case _           => dialectInKey(root).isDefined
      }
    }
  }

  private def isExternal(header: Option[String]) = header.exists(h => h.endsWith(">") && h.contains("|<"))
}
