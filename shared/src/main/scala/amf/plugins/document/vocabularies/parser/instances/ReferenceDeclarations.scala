package amf.plugins.document.vocabularies.parser.instances

import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.parser.FragmentRef
import amf.plugins.document.vocabularies.model.document.{DialectInstanceFragment, Vocabulary}
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, External}

import scala.collection.mutable

case class ReferenceDeclarations(references: mutable.Map[String, Any] = mutable.Map())(
    implicit ctx: DialectInstanceContext) {
  def +=(alias: String, unit: BaseUnit): Unit = {
    references += (alias -> unit)
    // useful for annotations
    unit match {
      case vocabulary: Vocabulary => ctx.declarations.registerUsedVocabulary(alias, vocabulary)
      case _                      =>
    }
    // register declared units properly
    unit match {
      case m: DeclaresModel =>
        val library = ctx.declarations.getOrCreateLibrary(alias)
        m.declares.foreach {
          case dialectElement: DialectDomainElement =>
            val localName = dialectElement.localRefName
            library.registerDialectDomainElement(localName, dialectElement)
            ctx.futureDeclarations.resolveRef(s"$alias.$localName", dialectElement)
          case decl => library += decl
        }
      case f: DialectInstanceFragment =>
        ctx.declarations.fragments += (alias -> FragmentRef(f.encodes, f.location()))
    }
  }

  def +=(external: External): Unit = {
    references += (external.alias.value()                 -> external)
    ctx.declarations.externals += (external.alias.value() -> external)
  }

  def baseUnitReferences(): Seq[BaseUnit] =
    references.values.toSet.filter(_.isInstanceOf[BaseUnit]).toSeq.asInstanceOf[Seq[BaseUnit]]
}
