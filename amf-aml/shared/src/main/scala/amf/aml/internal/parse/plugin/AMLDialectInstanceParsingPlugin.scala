package amf.aml.internal.parse.plugin

import amf.aml.client.scala.model.document.{Dialect, DialectInstance, kind}
import amf.aml.internal.AMLDialectInstancePlugin
import amf.aml.internal.parse.common.SyntaxExtensionsReferenceHandler
import amf.aml.internal.parse.hints.{DialectInstanceGuess, Guess}
import amf.aml.internal.parse.instances._
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler}
import amf.core.internal.parser._
import amf.core.internal.remote.Mimes._
import amf.core.internal.remote.{AmlDialectSpec, Mimes, Spec}
import org.mulesoft.common.core.Strings

/**
  * Parsing plugin for dialect instance like units derived from a resolved dialect
  * @param dialect resolved dialect
  */
class AMLDialectInstanceParsingPlugin(val dialect: Dialect)
    extends AMFParsePlugin
    with AMLDialectInstancePlugin[Root] {

  override val id: String = s"${dialect.nameAndVersion()}/dialect-instances-parsing-plugin"

  override def priority: PluginPriority = NormalPriority

  protected def guess: Guess[kind.DialectInstanceDocumentKind] = DialectInstanceGuess(dialect)

  override def parse(root: Root, ctx: ParserContext): BaseUnit = {
    val finder = DefaultNodeMappableFinder(ctx)
    val maybeUnit = guess.from(root) map {
      case kind.DialectInstanceFragment =>
        /**
          * Extract a name form the hint. Examples:
          * #%Library / Dialect 1.0               -> Library
          * #%My Fragment / My Test Dialect 1.0   -> My Fragment
          */
        val name = {
          val hint          = guess.hint(root).get // Should always be defined
          val normalizedStr = hint.stripPrefix("%").stripSpaces
          normalizedStr.substring(0, normalizedStr.indexOf("/"))
        }
        new DialectInstanceFragmentParser(root)(new DialectInstanceContext(dialect, finder, ctx)).parse(name)
      case kind.DialectInstanceLibrary =>
        new DialectInstanceLibraryParser(root)(new DialectInstanceContext(dialect, finder, ctx)).parse()
      case kind.DialectInstancePatch =>
        new DialectInstancePatchParser(root)(new DialectInstanceContext(dialect, finder, ctx).forPatch())
          .parse()
      case kind.DialectInstance =>
        new DialectInstanceParser(root)(new DialectInstanceContext(dialect, finder, ctx)).parseDocument()
      case _ =>
        DialectInstance()
    }
    maybeUnit.foreach(x => x.processingData.withSourceSpec(AmlDialectSpec(dialect.nameAndVersion())))
    maybeUnit.get
  }

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(eh)

  override def allowRecursiveReferences: Boolean = true

  override def applies(root: Root): Boolean = DialectInstanceGuess(dialect).from(root).isDefined

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] = Seq(Mimes.`application/yaml`, `application/json`)

  override def spec: Spec = AmlDialectSpec(dialect.nameAndVersion())

  /**
    * media types which specifies vendors that may be referenced.
    */
  override def validSpecsToReference: Seq[Spec] = Nil

  override def withIdAdoption: Boolean = false
}
