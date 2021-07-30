package amf.rdf.internal

import amf.core.client.common.{LowPriority, PluginPriority}
import amf.core.client.scala.parse.AMFSyntaxParsePlugin
import amf.core.client.scala.parse.document.{ParsedDocument, ParserContext}
import amf.core.internal.unsafe.PlatformSecrets
import org.mulesoft.common.io.Output

object RdfSyntaxPlugin extends AMFSyntaxParsePlugin with RdfPlatformSecrets {

  override val id = "Rdf"

  // TODO ARM to render syntax plugin
  def unparse[W: Output](mediaType: String, doc: ParsedDocument, writer: W): Option[W] =
    (doc, framework) match {
      case (input: RdfModelDocument, r) => r.rdfModelToSyntaxWriter(mediaType, input, writer)
      case _                            => None
    }
  override def parse(text: CharSequence, mediaType: String, ctx: ParserContext): ParsedDocument = {
    if (!ctx.parsingOptions.isAmfJsonLdSerialization) {
      framework.syntaxToRdfModel(mediaType, text)
    } else throw new UnsupportedOperationException
  }

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String]                 = Nil
  override def applies(element: CharSequence): Boolean = true
  override def priority: PluginPriority                = LowPriority
}
