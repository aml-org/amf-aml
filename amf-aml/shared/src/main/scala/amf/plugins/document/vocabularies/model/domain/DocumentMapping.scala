package amf.plugins.document.vocabularies.model.domain

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.domain.DocumentMappingModel
import amf.plugins.document.vocabularies.metamodel.domain.DocumentMappingModel.{
  DeclaredNodes,
  DocumentName,
  EncodedNode
}
import org.yaml.model.YNode

case class DocumentMapping(fields: Fields, annotations: Annotations) extends DomainElement {

  def documentName(): StrField                = fields.field(DocumentName)
  def encoded(): StrField                     = fields.field(EncodedNode)
  def declaredNodes(): Seq[PublicNodeMapping] = fields.field(DeclaredNodes)

  def withDocumentName(name: String): DocumentMapping   = set(DocumentName, name)
  def withEncoded(encodedNode: String): DocumentMapping = set(EncodedNode, encodedNode)
  def withDeclaredNodes(fragments: Seq[PublicNodeMapping]): DocumentMapping =
    setArrayWithoutId(DeclaredNodes, fragments)

  override def meta: DocumentMappingModel.type = DocumentMappingModel

  override def adopted(parent: String, cycle: Seq[String] = Seq()): this.type = {
    if (Option(id).isEmpty) {
      simpleAdoption(parent)
    }
    this
  }

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = ""
}

object DocumentMapping {
  def apply(): DocumentMapping                         = apply(Annotations())
  def apply(ast: YNode): DocumentMapping               = apply(Annotations(ast))
  def apply(annotations: Annotations): DocumentMapping = DocumentMapping(Fields(), annotations)
}
