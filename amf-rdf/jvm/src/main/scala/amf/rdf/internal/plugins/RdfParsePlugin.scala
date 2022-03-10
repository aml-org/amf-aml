package amf.rdf.internal.plugins

import amf.core.client.common.{LowPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.exception.UnsupportedParsedDocumentException
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler, SimpleReferenceHandler}
import amf.core.internal.parser.Root
import amf.core.internal.remote.{Mimes, Spec}
import amf.rdf.client.scala.RdfModelDocument
import amf.rdf.internal.unsafe.RdfPlatformSecrets
import amf.rdf.internal.{EntitiesFacade, RdfModelParser}

object RdfParsePlugin extends AMFParsePlugin with RdfPlatformSecrets {

  override def spec: Spec = Spec.AMF

  override def applies(element: Root): Boolean = true

  override def priority: PluginPriority = LowPriority

  override def parse(document: Root, ctx: ParserContext): BaseUnit = {
    document.parsed match {
      case RdfModelDocument(model) =>
        val rootNodeLocation = document.location
        val parser = RdfModelParser(ctx.config, new EntitiesFacade(ctx.config))
        parser.parse(model, rootNodeLocation)
      case _ => throw UnsupportedParsedDocumentException
    }
  }

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] = Seq(Mimes.`text/n3`)

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler = SimpleReferenceHandler

  override def allowRecursiveReferences: Boolean = false
}
