package amf.rdf.internal.plugins

import amf.core.client.common.{LowPriority, PluginPriority}
import amf.core.client.scala.parse.document.{ParsedDocument, StringParsedDocument}
import amf.core.client.scala.render.AMFSyntaxRenderPlugin
import amf.core.internal.remote.Mimes
import org.mulesoft.common.io.Output
import org.mulesoft.common.io.Output.OutputOps

object RdfSyntaxRenderPlugin extends AMFSyntaxRenderPlugin {

  override def emit[W: Output](mediaType: String, ast: ParsedDocument, writer: W): Option[W] = {
    ast match {
      case str: StringParsedDocument =>
        writer.append(str.ast.builder.toString)
        Some(writer)
      case _ => None
    }
  }

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] = Seq(Mimes.`text/n3`)

  override val id: String = "rdf-syntax-render-plugin"

  override def applies(element: ParsedDocument): Boolean = element.isInstanceOf[StringParsedDocument]

  override def priority: PluginPriority = LowPriority
}
