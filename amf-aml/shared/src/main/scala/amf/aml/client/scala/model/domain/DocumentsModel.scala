package amf.aml.client.scala.model.domain

import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.model.{BoolField, StrField}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.domain.DocumentsModelModel
import amf.aml.internal.metamodel.domain.DocumentsModelModel._
import org.yaml.model.YMap

case class DocumentsModel(fields: Fields, annotations: Annotations) extends DomainElement {
  def root(): DocumentMapping           = fields.field(Root)
  def library(): DocumentMapping        = fields.field(Library)
  def fragments(): Seq[DocumentMapping] = fields.field(Fragments)
  def selfEncoded(): BoolField          = fields.field(SelfEncoded)
  def declarationsPath(): StrField      = fields.field(DeclarationsPath)
  def keyProperty(): BoolField          = fields.field(KeyProperty)
  def referenceStyle(): StrField        = fields.field(ReferenceStyle)

  def withRoot(documentMapping: DocumentMapping): DocumentsModel     = set(Root, documentMapping)
  def withLibrary(library: DocumentMapping): DocumentsModel          = set(Library, library)
  def withFragments(fragments: Seq[DocumentMapping]): DocumentsModel = setArrayWithoutId(Fragments, fragments)
  def withSelfEncoded(selfEncoded: Boolean): DocumentsModel          = set(SelfEncoded, selfEncoded)
  def withDeclarationsPath(declarationsPath: String): DocumentsModel = set(DeclarationsPath, declarationsPath)
  def withKeyProperty(keyProperty: Boolean): DocumentsModel          = set(KeyProperty, keyProperty)
  def withReferenceStyle(referenceStyle: String): DocumentsModel     = set(ReferenceStyle, referenceStyle)

  override def meta: DocumentsModelModel.type = DocumentsModelModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = "/documents"
}

object DocumentsModel {
  def apply(): DocumentsModel                         = apply(Annotations())
  def apply(ast: YMap): DocumentsModel                = apply(Annotations(ast))
  def apply(annotations: Annotations): DocumentsModel = DocumentsModel(Fields(), annotations)
}
