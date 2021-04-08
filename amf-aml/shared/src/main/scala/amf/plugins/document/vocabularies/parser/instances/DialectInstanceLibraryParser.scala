package amf.plugins.document.vocabularies.parser.instances

import amf.core.Root
import amf.core.parser.Annotations
import amf.plugins.document.vocabularies.model.document.DialectInstanceLibrary

class DialectInstanceLibraryParser(root: Root)(implicit override val ctx: DialectInstanceContext)
    extends DialectInstanceParser(root) {

  def parse(): DialectInstanceLibrary = {
    val dialectInstance: DialectInstanceLibrary = DialectInstanceLibrary(Annotations(map))
      .withLocation(root.location)
      .withId(root.location)
      .withDefinedBy(ctx.dialect.id)

    parseDeclarations("library")

    val references =
      DialectInstanceReferencesParser(dialectInstance, map, root.references)
        .parse(dialectInstance.location().getOrElse(dialectInstance.id))

    if (ctx.declarations.externals.nonEmpty)
      dialectInstance.withExternals(ctx.declarations.externals.values.toSeq)

    if (ctx.declarations.declarables().nonEmpty)
      dialectInstance.withDeclares(ctx.declarations.declarables())

    if (references.baseUnitReferences().nonEmpty)
      dialectInstance.withReferences(references.baseUnitReferences())

    // resolve unresolved references
    ctx.futureDeclarations.resolve()

    dialectInstance
  }
}
