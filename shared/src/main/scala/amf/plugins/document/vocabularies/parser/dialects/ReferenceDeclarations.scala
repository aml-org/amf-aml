package amf.plugins.document.vocabularies.parser.dialects

import amf.core.model.document.{BaseUnit, DeclaresModel, RecursiveUnit}
import amf.core.parser.FragmentRef
import amf.plugins.document.vocabularies.model.document.{DialectFragment, Vocabulary}
import amf.plugins.document.vocabularies.model.domain.{ClassTerm, External, NodeMappable, PropertyTerm}

import scala.collection.mutable

case class ReferenceDeclarations(references: mutable.Map[String, Any] = mutable.Map())(implicit ctx: DialectContext) {
  def +=(alias: String, unit: BaseUnit): Unit = {
    references += (alias -> unit)
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
    }
  }

  def +=(external: External): Unit = {
    references += (external.alias.value()                 -> external)
    ctx.declarations.externals += (external.alias.value() -> external)
  }

  def baseUnitReferences(): Seq[BaseUnit] =
    references.values.toSet.filter(_.isInstanceOf[BaseUnit]).toSeq.asInstanceOf[Seq[BaseUnit]]
}
