package amf.aml.internal.parse.plugin

import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler}
import amf.core.internal.parser.Root
import amf.aml.internal.parse.common.SyntaxExtensionsReferenceHandler
import amf.aml.internal.parse.dialects.{DialectContext, DialectsParser}
import amf.aml.internal.parse.vocabularies.{VocabulariesParser, VocabularyContext}
import amf.aml.internal.parse.headers.{DialectHeader, ExtensionHeader}

class AMLVocabularyParsingPlugin extends AMFParsePlugin {

  override def parse(document: Root, ctx: ParserContext): BaseUnit = {
    val header = DialectHeader(document)

    header match {
      case Some(ExtensionHeader.VocabularyHeader) =>
        new VocabulariesParser(document)(new VocabularyContext(ctx)).parseDocument()
      case _ => throw new Exception("Dunno") // TODO: ARM - what to do with this
    }
  }

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(eh)

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
  override def mediaTypes: Seq[String] = Seq("application/aml", "application/yaml", "application/aml+yaml")

  /**
    * media types which specifies vendors that may be referenced.
    */
  override def validMediaTypesToReference: scala.Seq[String] =
    Seq("application/aml", "application/yaml", "application/aml+yaml")
}
