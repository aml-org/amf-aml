package amf.aml.internal.parse.plugin

import amf.aml.client.scala.model.document.kind
import amf.aml.internal.parse.common.SyntaxExtensionsReferenceHandler
import amf.aml.internal.parse.dialects.{DialectContext, DialectsParser}
import amf.aml.internal.parse.hints.DialectGuess
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

  override def parse(root: Root, ctx: ParserContext): BaseUnit = {

    DialectGuess.from(root) match {
      case Some(kind.DialectLibrary) =>
        new DialectsParser(root)(cleanDialectContext(ctx, root)).parseLibrary()
      case Some(kind.DialectFragment) =>
        new DialectsParser(root)(new DialectContext(ctx)).parseFragment()
      case Some(kind.Dialect) =>
        parseDialect(root, cleanDialectContext(ctx, root))
      case _ => throw CannotParseDocumentException("Cannot parse document as an AML Dialect")
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

  override def applies(root: Root): Boolean = DialectGuess.from(root).isDefined

  override def priority: PluginPriority = NormalPriority

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] = Seq(`application/yaml`)

  /**
    * media types which specifies vendors that may be referenced.
    */
  override def validSpecsToReference: Seq[Spec] = Seq(Spec.AML)

  override def withIdAdoption: Boolean = false
}
