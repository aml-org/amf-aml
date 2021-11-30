package amf.aml.internal.parse.plugin

import amf.aml.internal.parse.common.SyntaxExtensionsReferenceHandler
import amf.aml.internal.parse.headers.{DialectHeader, ExtensionHeader}
import amf.aml.internal.parse.plugin.error.CannotParseDocumentException
import amf.aml.internal.parse.vocabularies.{VocabulariesParser, VocabularyContext}
import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler}
import amf.core.internal.parser.Root
import amf.core.internal.remote.Mimes._
import amf.core.internal.remote.Spec

class AMLVocabularyParsingPlugin extends AMFParsePlugin {

  override def spec: Spec = Spec.AML

  override def parse(document: Root, ctx: ParserContext): BaseUnit = {
    val header = DialectHeader(document)

    header match {
      case Some(ExtensionHeader.VocabularyHeader) =>
        new VocabulariesParser(document)(new VocabularyContext(ctx)).parseDocument()
      case Some(header) => throw CannotParseDocumentException(s"Header $header is not a valid AML Vocabulary header")
      case _            => throw CannotParseDocumentException("Missing header for AML Vocabulary")
    }
  }

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(eh)

  override def referencePlugins: Seq[AMFParsePlugin] = List(this)

  override def allowRecursiveReferences: Boolean = true

  override val id: String = "vocabulary-parsing-plugin"

  override def applies(root: Root): Boolean = {
    DialectHeader(root) match {
      case Some(ExtensionHeader.VocabularyHeader) => true
      case _                                      => false
    }
  }

  override def priority: PluginPriority = NormalPriority

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] = Seq(`application/yaml`)

  override def withIdAdoption: Boolean = false
}
