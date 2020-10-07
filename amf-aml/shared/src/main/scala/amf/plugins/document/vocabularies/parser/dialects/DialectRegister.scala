package amf.plugins.document.vocabularies.parser.dialects

import amf.core.model.document.{DeclaresModel, RecursiveUnit}
import amf.core.model.domain.AmfObject
import amf.core.parser.{CollectionSideEffect, FragmentRef}
import amf.plugins.document.vocabularies.model.document.{DialectFragment, Vocabulary}
import amf.plugins.document.vocabularies.model.domain.{ClassTerm, External, NodeMappable, PropertyTerm}

case class DialectRegister()(implicit ctx: DialectContext) extends CollectionSideEffect[AmfObject] {
  override def onCollect(alias: String, unit: AmfObject): Unit = {
    unit match {
      case d: Vocabulary =>
        ctx.declarations.registerUsedVocabulary(alias, d) // to keep track of the uses: alias -> vocab, useful for annotations
        val library = ctx.declarations.getOrCreateLibrary(alias)
        d.declares.foreach {
          case prop: PropertyTerm => library.registerTerm(prop)
          case cls: ClassTerm     => library.registerTerm(cls)
        }
      case m: DeclaresModel =>
        val library = ctx.declarations.getOrCreateLibrary(alias)
        m.declares.foreach {
          case nodeMapping: NodeMappable => library.registerNodeMapping(nodeMapping)
          case decl                      => library += decl
        }
      case f: DialectFragment =>
        ctx.declarations.fragments += (alias -> FragmentRef(f.encodes, f.location()))
      case r: RecursiveUnit =>
        ctx.recursiveDeclarations = ctx.recursiveDeclarations.updated(alias, r)
      case external: External =>
        ctx.declarations.externals += (external.alias.value() -> external)
    }
  }
}
