package amf.plugins.document.vocabularies.parser.vocabularies
import amf.core.model.document.BaseUnit
import amf.plugins.document.vocabularies.model.document.Vocabulary
import amf.plugins.document.vocabularies.model.domain.{ClassTerm, External, PropertyTerm}

import scala.collection.mutable

case class ReferenceDeclarations(references: mutable.Map[String, Any] = mutable.Map())(
    implicit ctx: VocabularyContext) {
  def +=(alias: String, unit: BaseUnit): Unit = {
    references += (alias -> unit)
    val library = ctx.declarations.getOrCreateLibrary(alias)
    unit match {
      case d: Vocabulary =>
        ctx.registerVocabulary(alias, d)
        d.declares.foreach {
          case prop: PropertyTerm => library.registerTerm(prop)
          case cls: ClassTerm     => library.registerTerm(cls)
        }
    }
  }

  def +=(external: External): Unit = {
    references += (external.alias.value()                 -> external)
    ctx.declarations.externals += (external.alias.value() -> external)
  }

  def baseUnitReferences(): Seq[BaseUnit] =
    references.values.toSet.filter(_.isInstanceOf[BaseUnit]).toSeq.asInstanceOf[Seq[BaseUnit]]
}
