package amf.plugins.document.vocabularies.parser.vocabularies

import amf.core.model.domain.AmfObject
import amf.core.parser.CollectionSideEffect
import amf.plugins.document.vocabularies.model.document.Vocabulary
import amf.plugins.document.vocabularies.model.domain.{ClassTerm, External, PropertyTerm}

case class VocabularyRegister()(implicit ctx: VocabularyContext) extends CollectionSideEffect[AmfObject] {

  override def onCollect(alias: String, unit: AmfObject): Unit = {
    unit match {
      case vocab: Vocabulary => collectVocabulary(alias, vocab)
      case external: External => collectExternal(external)
    }
  }

  private def collectVocabulary(alias: String, vocab: Vocabulary): Unit = {
    val library = ctx.declarations.getOrCreateLibrary(alias)
    ctx.registerVocabulary(alias, vocab)
    vocab.declares.foreach {
      case prop: PropertyTerm => library.registerTerm(prop)
      case cls: ClassTerm     => library.registerTerm(cls)
    }
  }

  private def collectExternal(external: External): Unit = {
    ctx.declarations.externals += (external.alias.value() -> external)
  }
}