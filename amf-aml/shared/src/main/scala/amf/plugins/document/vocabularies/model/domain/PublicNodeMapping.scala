package amf.plugins.document.vocabularies.model.domain

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.domain.PublicNodeMappingModel
import amf.plugins.document.vocabularies.metamodel.domain.PublicNodeMappingModel._
import org.yaml.model.YMapEntry

case class PublicNodeMapping(fields: Fields, annotations: Annotations) extends DomainElement {

  def name(): StrField       = fields.field(Name)
  def mappedNode(): StrField = fields.field(MappedNode)

  def withName(name: String): PublicNodeMapping             = set(Name, name)
  def withMappedNode(mappedNode: String): PublicNodeMapping = set(MappedNode, mappedNode)

  override def meta: PublicNodeMappingModel.type = PublicNodeMappingModel

  override def adopted(parent: String, cycle: Seq[String] = Seq()): this.type = {
    if (Option(id).isEmpty) {
      simpleAdoption(parent)
    }
    this
  }

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = ""
}

object PublicNodeMapping {
  def apply(): PublicNodeMapping                         = apply(Annotations())
  def apply(ast: YMapEntry): PublicNodeMapping           = apply(Annotations(ast))
  def apply(annotations: Annotations): PublicNodeMapping = PublicNodeMapping(Fields(), annotations)
}
