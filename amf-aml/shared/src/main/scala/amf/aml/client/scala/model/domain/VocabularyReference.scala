package amf.aml.client.scala.model.domain

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.internal.utils.AmfStrings
import amf.aml.internal.metamodel.domain.VocabularyReferenceModel
import amf.aml.internal.metamodel.domain.VocabularyReferenceModel._
import org.yaml.model.YMap

case class VocabularyReference(fields: Fields, annotations: Annotations) extends DomainElement {

  def alias: StrField     = fields.field(Alias)
  def reference: StrField = fields.field(Reference)
  def base: StrField      = fields.field(Base)

  def withAlias(alias: String): VocabularyReference         = set(Alias, alias)
  def withReference(reference: String): VocabularyReference = set(Reference, reference)
  def withBase(base: String): VocabularyReference           = set(Base, base)

  override def meta: VocabularyReferenceModel.type = VocabularyReferenceModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = alias.option() match {
    case Some(alias) => "/vocabularyReference/" + alias.urlComponentEncoded
    case None        => throw new Exception("Cannot set ID of VocabularyReference without alias")
  }
}

object VocabularyReference {
  def apply(): VocabularyReference                         = apply(Annotations())
  def apply(ast: YMap): VocabularyReference                = apply(Annotations(ast))
  def apply(annotations: Annotations): VocabularyReference = VocabularyReference(Fields(), annotations)
}
