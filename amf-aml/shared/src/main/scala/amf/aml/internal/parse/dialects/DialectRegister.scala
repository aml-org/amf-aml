package amf.aml.internal.parse.dialects

import amf.core.client.scala.model.document.{DeclaresModel, RecursiveUnit}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.parse.document.CollectionSideEffect
import amf.core.internal.parser.domain.FragmentRef
import amf.aml.client.scala.model.document.{DialectFragment, Vocabulary}
import amf.aml.client.scala.model.domain.{ClassTerm, External, NodeMappable, PropertyTerm}

case class DialectRegister()(implicit ctx: DialectContext) extends CollectionSideEffect[AmfObject] {
  type NodeMappable = NodeMappable.AnyNodeMappable
  override def onCollect(alias: String, unit: AmfObject): Unit = {
    unit match {
      case d: Vocabulary =>
        ctx.declarations
          .registerUsedVocabulary(alias, d) // to keep track of the uses: alias -> vocab, useful for annotations
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
