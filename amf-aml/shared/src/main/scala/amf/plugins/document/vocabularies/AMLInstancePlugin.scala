package amf.plugins.document.vocabularies

import amf.client.remod.amfcore.plugins.{HighPriority, PluginPriority}
import amf.client.remod.amfcore.plugins.parse.{AMFParsePlugin, ParsingInfo}
import amf.core.Root
import amf.core.client.ParsingOptions
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.parser.{ParserContext, ReferenceHandler}
import amf.plugins.document.vocabularies.model.document.Dialect

class AMLInstancePlugin(dialect: Dialect) extends AMFParsePlugin {

  override def parse(document: Root, ctx: ParserContext, options: ParsingOptions): BaseUnit =
    throw new UnsupportedOperationException

  override def referenceHandler(eh: ErrorHandler): ReferenceHandler = ???

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] = ???

  /**
    * media types which specifies vendors that may be referenced.
    */
  override def validMediaTypesToReference: Seq[String] = Nil

  override def allowRecursiveReferences: Boolean = true

  override def applies(element: ParsingInfo): Boolean = false

  override def priority: PluginPriority = HighPriority

  override val id: String = s"MetaPlugin-${dialect.nameAndVersion()}"
}
