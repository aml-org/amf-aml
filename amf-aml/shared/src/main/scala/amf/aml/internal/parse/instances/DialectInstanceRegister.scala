package amf.aml.internal.parse.instances

import amf.core.client.scala.model.document.DeclaresModel
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.parse.document.CollectionSideEffect
import amf.core.internal.parser.domain.FragmentRef
import amf.aml.client.scala.model.document.{DialectInstanceFragment, Vocabulary}
import amf.aml.client.scala.model.domain.{DialectDomainElement, External}

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
      case fragment: DialectInstanceFragment =>
        ctx.declarations.fragments += (alias -> FragmentRef(fragment.encodes, fragment.location()))
      case external: External => ctx.declarations.externals += (external.alias.value() -> external)
    }
  }
}
