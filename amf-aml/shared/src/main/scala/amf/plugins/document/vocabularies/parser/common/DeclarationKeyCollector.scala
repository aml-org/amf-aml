package amf.plugins.document.vocabularies.parser.common

import amf.core.metamodel.document.DocumentModel
import amf.core.model.document.DeclaresModel
import amf.core.model.domain.{AmfArray, Annotation, DomainElement}
import amf.core.parser.{Annotations, Declarations}
import org.yaml.model.YMapEntry

trait DeclarationKeyCollector {

  private var declarationKeys: List[DeclarationKey] = List.empty

  def addDeclarationKey(key: DeclarationKey): Unit = {
    declarationKeys = key :: declarationKeys
  }

  protected def addDeclarationsToModel(model: DeclaresModel)(implicit ctx: DeclarationContext): Unit =
    addDeclarationsToModel(model, ctx.declarations.declarables())

  protected def addDeclarationsToModel(model: DeclaresModel, declares: Seq[DomainElement]): Unit = {
    val ann = Annotations(DeclarationKeys(declarationKeys)) ++= Annotations.virtual() ++= Annotations.inferred()
    // check declaration key to use as source maps for field and value
    if (declares.nonEmpty || declarationKeys.nonEmpty)
      model.setWithoutId(DocumentModel.Declares, AmfArray(declares, Annotations.virtual()), ann)

  }
}

trait DeclarationContext {
  val declarations: Declarations
}

case class DeclarationKeys(keys: List[DeclarationKey]) extends Annotation

case class DeclarationKey(entry: YMapEntry, isAbstract: Boolean)

object DeclarationKey {
  def apply(entry: YMapEntry, isAbstract: Boolean = false): DeclarationKey =
    new DeclarationKey(entry, isAbstract = isAbstract)
}
