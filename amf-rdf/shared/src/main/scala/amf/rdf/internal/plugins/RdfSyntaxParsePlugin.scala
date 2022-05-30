package amf.rdf.internal.plugins

import amf.core.client.common.{LowPriority, PluginPriority}
import amf.core.client.scala.parse.AMFSyntaxParsePlugin
import amf.core.client.scala.parse.document.{ParsedDocument, ParserContext}
import amf.core.internal.remote.Mimes
import amf.rdf.internal.unsafe.RdfPlatformSecrets

object RdfSyntaxParsePlugin extends AMFSyntaxParsePlugin with RdfPlatformSecrets {

  override val id = "rdf-syntax-parse-plugin"

  override def parse(text: CharSequence, mediaType: String, ctx: ParserContext): ParsedDocument = {
    if (!ctx.parsingOptions.isAmfJsonLdSerialization) {
      framework.syntaxToRdfModel(mediaType, text)
    } else throw new UnsupportedOperationException
  }

  override def mainMediaType: String = Mimes.`text/n3`

  /** media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String]                 = Seq(Mimes.`text/n3`)
  override def applies(element: CharSequence): Boolean = true
  override def priority: PluginPriority                = LowPriority
}
