package amf.client.remod.parsing

import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.plugins.parse.AMFParsePlugin
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.core.Root
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.parser.{EmptyFutureDeclarations, ParserContext, ReferenceHandler}

import amf.plugins.document.vocabularies.parser.common.SyntaxExtensionsReferenceHandler
import amf.plugins.document.vocabularies.parser.dialects.{DialectContext, DialectsParser}
import amf.plugins.document.vocabularies.parser.vocabularies.{VocabulariesParser, VocabularyContext}
import amf.plugins.document.vocabularies.plugin.headers.{DialectHeader, ExtensionHeader}

class AMLDialectParsingPlugin extends AMFParsePlugin {
  val knownHeaders =
    IndexedSeq(ExtensionHeader.DialectHeader,
               ExtensionHeader.DialectFragmentHeader,
               ExtensionHeader.DialectLibraryHeader)

  override def parse(document: Root, ctx: ParserContext): BaseUnit = {
    val header = DialectHeader(document)

    header match {
      case Some(ExtensionHeader.DialectLibraryHeader) =>
        new DialectsParser(document)(cleanDialectContext(ctx, document)).parseLibrary()
      case Some(ExtensionHeader.DialectFragmentHeader) =>
        new DialectsParser(document)(new DialectContext(ctx)).parseFragment()
      case Some(ExtensionHeader.DialectHeader) =>
        parseDialect(document, cleanDialectContext(ctx, document))
      case _ => throw new Exception("Dunno") // TODO: ARM - what to do with this
    }
  }

  private def parseDialect(document: Root, parentContext: ParserContext) =
    new DialectsParser(document)(new DialectContext(parentContext)).parseDocument()

  protected def cleanDialectContext(wrapped: ParserContext, root: Root): DialectContext = {
    val cleanNested =
      ParserContext(root.location, root.references, EmptyFutureDeclarations(), wrapped.config)
    new DialectContext(cleanNested)
  }

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(eh)

  override def allowRecursiveReferences: Boolean = true

  override val id: String = "dialect-parsing-plugin"

  override def applies(root: Root): Boolean = {
    DialectHeader(root) match {
      case Some(header) => knownHeaders.contains(header)
      case _            => false
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
