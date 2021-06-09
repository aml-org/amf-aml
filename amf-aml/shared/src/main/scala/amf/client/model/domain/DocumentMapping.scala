package amf.client.model.domain

import amf.client.convert.VocabulariesClientConverter._
import amf.client.model.StrField
import amf.plugins.document.vocabularies.model.domain.{DocumentMapping => InternalDocumentMapping}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class DocumentMapping(override private[amf] val _internal: InternalDocumentMapping) extends DomainElement {

  @JSExportTopLevel("model.domain.DocumentMapping")
  def this() = this(InternalDocumentMapping())

  def documentName(): StrField = _internal.documentName()
  def withDocumentName(name: String): DocumentMapping = {
    _internal.withDocumentName(name)
    this
  }
  def encoded(): StrField = _internal.encoded()
  def withEncoded(encodedNode: String): DocumentMapping = {
    _internal.withEncoded(encodedNode)
    this
  }
  def declaredNodes(): ClientList[PublicNodeMapping] = _internal.declaredNodes().asClient
  def withDeclaredNodes(declarations: ClientList[PublicNodeMapping]): DocumentMapping = {
    _internal.withDeclaredNodes(declarations.asInternal)
  }
}
