package amf.aml.internal.parse.hints

import amf.core.internal.parser.Root
import org.mulesoft.common.core._

object DialectHeader {

  /** Fetch header or dialect directive. */
  def apply(root: Root): Option[String] = {
    YamlDirectiveComment
      .from(root)
      .orElse($DialectPropertyValue.from(root).map(metaText => s"%$metaText"))
      .map(_.stripSpaces)
  }

  /**
    * Extract the root part from a dialect header directive
    * e.g.
    *   Header directive                         Root part
    *   #%Library / Dialect 1.0               -> Library
    *   #%My Fragment / My Test Dialect 1.0   -> My Fragment
    */
  def root(document: Root): Option[String] = {
    for {
      header <- DialectHeader(document)
    } yield {
      header.substring(1, header.indexOf("/"))
    }
  }
}
