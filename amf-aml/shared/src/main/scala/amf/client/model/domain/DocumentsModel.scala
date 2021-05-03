package amf.client.model.domain

import amf.client.convert.VocabulariesClientConverter._
import amf.client.model.{BoolField, StrField}
import amf.plugins.document.vocabularies.model.domain.{DocumentsModel => InternalDocumentsModel}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class DocumentsModel(override private[amf] val _internal: InternalDocumentsModel) extends DomainElement {

  @JSExportTopLevel("model.domain.DocumentsModel")
  def this() = this(InternalDocumentsModel())

  def root(): DocumentMapping = DocumentMapping(_internal.root())
  def withRoot(documentMapping: DocumentMapping): InternalDocumentsModel = {
    _internal.withRoot(documentMapping._internal)
  }
  def fragments(): ClientList[DocumentMapping] = _internal.fragments().asClient
  def withFragments(fragments: ClientList[DocumentMapping]): InternalDocumentsModel = {
    _internal.withFragments(fragments.asInternal)
  }
  def library(): DocumentMapping = DocumentMapping(_internal.library())
  def withLibrary(library: DocumentMapping): InternalDocumentsModel = {
    _internal.withLibrary(library._internal)
  }

  def selfEncoded(): BoolField = _internal.selfEncoded()
  def withSelfEncoded(selfEncoded: Boolean): DocumentsModel = {
    _internal.withSelfEncoded(selfEncoded)
  }

  def declarationsPath(): StrField = _internal.declarationsPath()
  def withDeclarationsPath(declarationsPath: String): DocumentsModel = {
    _internal.withDeclarationsPath(declarationsPath)
  }

  def keyProperty(): BoolField = _internal.keyProperty()
  def withKeyProperty(keyProperty: Boolean): DocumentsModel = {
    _internal.withKeyProperty(keyProperty)
  }
}
