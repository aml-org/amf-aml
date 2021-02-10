package amf.client.`new`

import amf.client.`new`.amfcore.{AmfParsePlugin, NormalPriority, PluginPriority}
import amf.core.Root
import amf.core.errorhandling.ErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.parser.{ParserContext, ReferenceHandler}
import amf.core.remote.Vendor
import amf.plugins.document.vocabularies.model.document.Dialect
import org.yaml.model.YDocument


object AmlMetaParsePlugin extends AmfParsePlugin{
  override def parse(document: Root, ctx: ParserContext): BaseUnit = ???

  override val supportedVendors: Seq[Vendor] = AML
  override val validVendorsToReference: Seq[Vendor] = _

  override def referenceHandler(eh: ErrorHandler): ReferenceHandler = ???

  override def allowRecursiveReferences: Boolean = ???

  override val id: String = "AML1.0"

  override def applies(element: YDocument): Boolean = ???

  override def priority: PluginPriority = ???
}

class AmlParsePlugin(dialect:Dialect) extends AmfParsePlugin {
  override def parse(document: Root, ctx: ParserContext): BaseUnit = ???

  override val supportedVendors: Seq[Vendor] = _
  override val validVendorsToReference: Seq[Vendor] = _

  override def referenceHandler(eh: ErrorHandler): ReferenceHandler = ???

  override def allowRecursiveReferences: Boolean = ???

  override val id: String = "AML/" + dialect.nameAndVersion()

  override def applies(element: YDocument): Boolean =

  override def priority: PluginPriority = NormalPriority
}
