package amf.plugins.document.vocabularies.parser.instances

import amf.core.model.document.DeclaresModel
import amf.core.model.domain.AmfObject
import amf.core.parser.{CollectionSideEffect, FragmentRef}
import amf.plugins.document.vocabularies.model.document.{DialectInstanceFragment, Vocabulary}
import amf.plugins.document.vocabularies.model.domain.{DialectDomainElement, External}

case class DialectInstanceRegister()(implicit ctx: DialectInstanceContext) extends CollectionSideEffect[AmfObject] {
  override def onCollect(alias: String, unit: AmfObject): Unit = {

    // register declared units properly
    unit match {
      case vocabulary: Vocabulary => ctx.declarations.registerUsedVocabulary(alias, vocabulary)
      case dialect: DeclaresModel =>
        val library = ctx.declarations.getOrCreateLibrary(alias)
        dialect.declares.foreach {
          case dialectElement: DialectDomainElement =>
            val localName = dialectElement.localRefName
            library.registerDialectDomainElement(localName, dialectElement)
            ctx.futureDeclarations.resolveRef(s"$alias.$localName", dialectElement)
          case decl => library += decl
        }
      case fragment: DialectInstanceFragment => ctx.declarations.fragments += (alias -> FragmentRef(fragment.encodes, fragment.location()))
      case external: External => ctx.declarations.externals += (external.alias.value() -> external)
    }
  }
}