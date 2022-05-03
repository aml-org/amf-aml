package amf.aml.internal.parse.plugin

import amf.aml.client.scala.model.document.kind
import amf.aml.internal.parse.common.SyntaxExtensionsReferenceHandler
import amf.aml.internal.parse.hints.VocabularyGuess
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

  override def parse(root: Root, ctx: ParserContext): BaseUnit = {

    VocabularyGuess.from(root) match {
      case Some(kind.Vocabulary) =>
        new VocabulariesParser(root)(new VocabularyContext(ctx)).parseDocument()
      case _ => throw CannotParseDocumentException("Cannot parse document as an AML Vocabulary")
    }
  }

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(eh)

  override def allowRecursiveReferences: Boolean = true

  override val id: String = "vocabulary-parsing-plugin"

  override def applies(root: Root): Boolean = VocabularyGuess.from(root).isDefined

  override def priority: PluginPriority = NormalPriority

  /** media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] = Seq(`application/yaml`, `application/json`)

  /** media types which specifies vendors that may be referenced.
    */
  override def validSpecsToReference: scala.Seq[Spec] = Seq(Spec.AML)

  override def withIdAdoption: Boolean = false
}
