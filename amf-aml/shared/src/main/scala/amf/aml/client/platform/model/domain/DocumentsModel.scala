package amf.aml.client.platform.model.domain

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.domain.DomainElement
import amf.core.client.scala.model.{BoolField, StrField}
import amf.aml.client.scala.model.domain.{DocumentsModel => InternalDocumentsModel}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class DocumentsModel(override private[amf] val _internal: InternalDocumentsModel) extends DomainElement {

  @JSExportTopLevel("DocumentsModel")
  def this() = this(InternalDocumentsModel())

  def root(): DocumentMapping = DocumentMapping(_internal.root())
  def withRoot(documentMapping: DocumentMapping): DocumentsModel = {
    _internal.withRoot(documentMapping._internal)
  }
  def fragments(): ClientList[DocumentMapping] = _internal.fragments().asClient
  def withFragments(fragments: ClientList[DocumentMapping]): DocumentsModel = {
    _internal.withFragments(fragments.asInternal)
  }
  def library(): DocumentMapping = DocumentMapping(_internal.library())
  def withLibrary(library: DocumentMapping): DocumentsModel = {
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
