package amf.aml.internal.parse.plugin

import amf.aml.internal.parse.common.SyntaxExtensionsReferenceHandler
import amf.aml.internal.parse.dialects.{DialectContext, DialectsParser}
import amf.aml.internal.parse.headers.{DialectHeader, ExtensionHeader}
import amf.aml.internal.parse.plugin.error.CannotParseDocumentException
import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{EmptyFutureDeclarations, ParserContext, ReferenceHandler}
import amf.core.internal.parser.Root
import amf.core.internal.remote.Mimes._
import amf.core.internal.remote.Spec

class AMLDialectParsingPlugin extends AMFParsePlugin {

  override def spec: Spec = Spec.AML

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
      case Some(header) => throw CannotParseDocumentException(s"Header $header is not a valid AML Dialect header")
      case _            => throw CannotParseDocumentException("Missing header for AML Dialect")
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
  override def mediaTypes: Seq[String] = Seq(`application/yaml`)

  override def referencePlugins: Seq[AMFParsePlugin] = List(this, new AMLVocabularyParsingPlugin())

  override def withIdAdoption: Boolean = false
}
