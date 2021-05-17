package amf.client.remod.parsing

import amf.client.remod.amfcore.plugins.parse.AMFParsePlugin
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.core.Root
import amf.core.client.ParsingOptions
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.parser.{ParserContext, ReferenceHandler}
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.parser.common.SyntaxExtensionsReferenceHandler
import amf.plugins.document.vocabularies.plugin.headers.{DialectHeader, ExtensionHeader}

class AMLVocabularyParsingPlugin extends AMFParsePlugin {
  override def parse(document: Root, ctx: ParserContext, options: ParsingOptions): BaseUnit =
    AMLPlugin.parse(document, ctx, options)

  override def referenceHandler(eh: ErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(AMLPlugin.registry, eh)

  override def allowRecursiveReferences: Boolean = true

  override val id: String = "vocabulary-parsing-plugin"

  override def applies(root: Root): Boolean = {
    DialectHeader.dialectHeaderDirective(root) match {
      case Some(ExtensionHeader.VocabularyHeader) => true
      case _                                      => false
    }
  }

  override def priority: PluginPriority = NormalPriority

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] = Seq("application/aml", "application/yaml", "application/aml+yaml")

  /**
    * media types which specifies vendors that may be referenced.
    */
  override def validMediaTypesToReference: scala.Seq[String] =
    Seq("application/aml", "application/yaml", "application/aml+yaml")
}
