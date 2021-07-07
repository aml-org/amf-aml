package amf.aml.internal.parse.plugin

import amf.core.client.common.{NormalPriority, PluginPriority}
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.parse.AMFParsePlugin
import amf.core.client.scala.parse.document.{ParserContext, ReferenceHandler, SyamlParsedDocument}
import amf.core.internal.parser._
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.aml.client.scala.model.document.{Dialect, DialectInstance, kind}
import amf.aml.internal.AMLDialectInstancePlugin
import amf.aml.internal.parse.common.SyntaxExtensionsReferenceHandler
import amf.aml.internal.parse.instances._
import amf.aml.internal.parse.headers.DialectHeader
import org.yaml.model.YMap

/**
  * Parsing plugin for dialect instance like units derived from a resolved dialect
  * @param dialect resolved dialect
  */
class AMLDialectInstanceParsingPlugin(val dialect: Dialect)
    extends AMFParsePlugin
    with AMLDialectInstancePlugin[Root] {

  override val id: String = s"${dialect.id}/dialect-instances-parsing-plugin"

  override def priority: PluginPriority = NormalPriority

  override def parse(document: Root, ctx: ParserContext): BaseUnit = {
    val finder = DefaultNodeMappableFinder(ctx)
    val maybeUnit = documentKindFor(document) map {
      case kind.DialectInstanceFragment =>
        val name = DialectHeader.root(document).get // Should always be defined
        new DialectInstanceFragmentParser(document)(new DialectInstanceContext(dialect, finder, ctx)).parse(name)
      case kind.DialectInstanceLibrary =>
        new DialectInstanceLibraryParser(document)(new DialectInstanceContext(dialect, finder, ctx)).parse()
      case kind.DialectInstancePatch =>
        new DialectInstancePatchParser(document)(new DialectInstanceContext(dialect, finder, ctx).forPatch())
          .parse()
      case kind.DialectInstance =>
        new DialectInstanceParser(document)(new DialectInstanceContext(dialect, finder, ctx)).parseDocument()
      case _ =>
        DialectInstance()
    }
    maybeUnit.get
  }

  override def referenceHandler(eh: AMFErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(eh)

  override def allowRecursiveReferences: Boolean = true

  override def applies(root: Root): Boolean = documentKindFor(root).isDefined

  private def documentKindFor(root: Root): Option[kind.DialectInstanceDocumentKind] = {
    if (dialect.usesKeyPropertyMatching) {
      matchByKeyProperty(root)
    } else {
      matchByHeader(root)
    }
  }

  private def matchByHeader(root: Root): Option[kind.DialectInstanceDocumentKind] = {
    for {
      header <- DialectHeader(root)
      kind <- {
        header.split("\\|") match {
          case Array(header, dialectUri) =>
            matchByHeaderForInlineDialect(header, dialectUri)
          case _ =>
            dialect.documentKindFor(header)
        }
      }
    } yield {
      kind
    }
  }

  private def matchByHeaderForInlineDialect(header: String, dialectUri: String) = {
    if (dialectUri.stripPrefix("<").stripSuffix(">") == dialect.id) {
      dialect.documentKindFor(header)
    } else {
      None
    }
  }

  private def matchByKeyProperty(root: Root): Option[kind.DialectInstanceDocumentKind] = {
    val name    = dialect.name().value()
    val version = dialect.version().value()
    root.parsed match {
      case SyamlParsedDocument(document, _) if document.toOption[YMap].exists(_.hasEntry(name, version)) =>
        Some(kind.DialectInstance) // We can only define dialect instances (not fragments, etc.) with key properties
      case _ =>
        None
    }
  }

  /**
    * media types which specifies vendors that are parsed by this plugin.
    */
  override def mediaTypes: Seq[String] =
    Seq("application/aml", "application/yaml", "application/aml+yaml", "application/json", "application/aml+json")

  /**
    * media types which specifies vendors that may be referenced.
    */
  override def validMediaTypesToReference: Seq[String] =
    Seq("application/aml", "application/yaml", "application/aml+yaml", "application/json", "application/aml+json")
}