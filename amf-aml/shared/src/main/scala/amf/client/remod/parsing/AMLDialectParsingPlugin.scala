package amf.client.remod.parsing

import amf.client.remod.amfcore.plugins.parse.{AMFParsePlugin, ParsingInfo}
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.core.Root
import amf.core.client.ParsingOptions
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.parser.{ParserContext, ReferenceHandler}
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.parser.common.SyntaxExtensionsReferenceHandler
import amf.plugins.document.vocabularies.parser.dialects.{DialectContext, DialectsParser}
import amf.plugins.document.vocabularies.plugin.headers.{DialectHeader, ExtensionHeader}

class AMLDialectParsingPlugin extends AMFParsePlugin {
  val knownHeaders =
    IndexedSeq(ExtensionHeader.DialectHeader,
               ExtensionHeader.DialectFragmentHeader,
               ExtensionHeader.DialectLibraryHeader)

  override def parse(document: Root, ctx: ParserContext, options: ParsingOptions): BaseUnit =
    AMLPlugin.parse(document, ctx, options)

  override def referenceHandler(eh: ErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(AMLPlugin.registry, eh)

  override def allowRecursiveReferences: Boolean = true

  override val id: String = "dialect-parsing-plugin"

  override def applies(info: ParsingInfo): Boolean = {
    DialectHeader.dialectHeaderDirective(info.parsed) match {
      case Some(header) => knownHeaders.contains(header)
      case _            => false
    }
  }

  override def priority: PluginPriority = NormalPriority
/**
    * media types which specifies vendors that are parsed by this plugin.
    */
override def mediaTypes: Seq[String] = Seq("application/aml")
/**
    * media types which specifies vendors that may be referenced.
    */
override def validMediaTypesToReference: scala.Seq[String] = Seq("application/aml")
}
