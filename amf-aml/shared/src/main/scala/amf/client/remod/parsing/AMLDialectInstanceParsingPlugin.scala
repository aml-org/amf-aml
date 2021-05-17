package amf.client.remod.parsing

import amf.client.remod.AMLDialectInstancePlugin
import amf.client.remod.amfcore.plugins.parse.AMFParsePlugin
import amf.client.remod.amfcore.plugins.{NormalPriority, PluginPriority}
import amf.core.Root
import amf.core.client.ParsingOptions
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.parser.{ParserContext, ReferenceHandler, SyamlParsedDocument, YMapOps, YNodeLikeOps}
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, kind}
import amf.plugins.document.vocabularies.parser.common.SyntaxExtensionsReferenceHandler
import amf.plugins.document.vocabularies.parser.instances._
import amf.plugins.document.vocabularies.plugin.headers.DialectHeader
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

  override def parse(document: Root, ctx: ParserContext, options: ParsingOptions): BaseUnit = {
    val maybeUnit = documentKindFor(document) map {
      case kind.DialectInstanceFragment =>
        val name = DialectHeader.dialectHeaderDirectiveRootPart(document).get // Should always be defined
        new DialectInstanceFragmentParser(document)(new DialectInstanceContext(dialect, ctx)).parse(name)

      case kind.DialectInstanceLibrary =>
        new DialectInstanceLibraryParser(document)(new DialectInstanceContext(dialect, ctx)).parse()

      case kind.DialectInstancePatch =>
        new DialectInstancePatchParser(document)(new DialectInstanceContext(dialect, ctx).forPatch()).parse()

      case kind.DialectInstance =>
        new DialectInstanceParser(document)(new DialectInstanceContext(dialect, ctx)).parseDocument()

      case _ =>
        DialectInstance()
    }
    maybeUnit.get
  }

  override def referenceHandler(eh: ErrorHandler): ReferenceHandler =
    new SyntaxExtensionsReferenceHandler(AMLPlugin.registry, eh)

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
      header <- DialectHeader.dialectHeaderDirective(root)
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
