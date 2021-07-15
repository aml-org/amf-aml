package amf.aml.client.scala.model.domain

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar, DomainElement}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.domain.ClassTermModel
import amf.aml.internal.metamodel.domain.ClassTermModel._
import org.yaml.model.YMap

case class ClassTerm(fields: Fields, annotations: Annotations) extends DomainElement {
  override def meta: ClassTermModel.type = ClassTermModel

  override def adopted(parent: String, cycle: Seq[String] = Seq()): this.type = {
    if (Option(id).isEmpty) {
      simpleAdoption(parent)
    }
    this
  }

  def name: StrField            = fields.field(Name)
  def displayName: StrField     = fields.field(DisplayName)
  def description: StrField     = fields.field(Description)
  def properties: Seq[StrField] = fields.field(Properties)
  def subClassOf: Seq[StrField] = fields.field(SubClassOf)

  def withName(name: String): ClassTerm                    = set(Name, name)
  def withDisplayName(displayName: String): ClassTerm      = set(DisplayName, displayName)
  def withDescription(description: String): ClassTerm      = set(Description, description)
  def withProperties(properties: Seq[String]): ClassTerm   = set(Properties, AmfArray(properties.map(AmfScalar(_))))
  def withSubClassOf(superClasses: Seq[String]): ClassTerm = set(SubClassOf, AmfArray(superClasses.map(AmfScalar(_))))

  /** Value , path + field value that is used to compose the id when the object its adopted */
  private[amf] override def componentId: String = ""
}

object ClassTerm {
  def apply(): ClassTerm = apply(Annotations())

  def apply(ast: YMap): ClassTerm = apply(Annotations(ast))

  def apply(annotations: Annotations): ClassTerm = ClassTerm(Fields(), annotations)
}
