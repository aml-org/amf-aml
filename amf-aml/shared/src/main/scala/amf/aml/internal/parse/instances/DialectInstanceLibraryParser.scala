package amf.aml.internal.parse.instances

import amf.core.internal.parser.Root
import amf.core.internal.parser.domain.Annotations
import amf.aml.client.scala.model.document.{DialectInstanceLibrary, DialectInstanceProcessingData}
import com.github.ghik.silencer.silent

class DialectInstanceLibraryParser(root: Root)(implicit override val ctx: DialectInstanceContext)
    extends DialectInstanceParser(root) {

  def parse(): DialectInstanceLibrary = {
    @silent("deprecated") // Silent can only be used in assignment expressions
    val dialectInstance: DialectInstanceLibrary = DialectInstanceLibrary(Annotations(map))
      .withLocation(root.location)
      .withId(root.location)
      .withProcessingData(
        DialectInstanceProcessingData()
          .withTransformed(false)
          .withDefinedBy(ctx.dialect.id)
          .adopted(root.location + "#")
      )
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
