package amf.plugins.document.vocabularies

import amf.client.remod.amfcore.plugins.PluginPriority
import amf.client.remod.amfcore.plugins.parse.{AMFParsePlugin, ParsingInfo}
import amf.core.Root
import amf.core.client.ParsingOptions
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.parser.{ParserContext, ReferenceHandler}
import amf.plugins.document.vocabularies.model.document.Dialect

class AMLInstancePlugin(dialect:Dialect) extends AMFParsePlugin{

  override def parse(document: Root, ctx: ParserContext, options: ParsingOptions): Option[BaseUnit] = {

  }

  override def referenceHandler(eh: ErrorHandler): ReferenceHandler = ???

  override def allowRecursiveReferences: Boolean = ???

  override val supportedVendors: Seq[String] = _
  override val validVendorsToReference: Seq[String] = _
  override val id: String = _

  override def applies(element: ParsingInfo): Boolean = checkDialect

  override def priority: PluginPriority = ???
}
