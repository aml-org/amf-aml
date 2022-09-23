package amf.aml.client.scala.model.domain

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar, DomainElement}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.domain.ObjectPropertyTermModel._
import amf.aml.internal.metamodel.domain.{DatatypePropertyTermModel, ObjectPropertyTermModel}
import org.yaml.model.YMap

abstract class PropertyTerm extends DomainElement {

  override def adopted(parent: String, cycle: Seq[String] = Seq()): PropertyTerm.this.type = {
    if (Option(id).isEmpty) {
      simpleAdoption(parent)
    }
    this
  }

  override def componentId: String = ""
  def name: StrField               = fields.field(Name)
  def displayName: StrField        = fields.field(DisplayName)
  def description: StrField        = fields.field(Description)
  def range: StrField              = fields.field(Range)
  def subPropertyOf: Seq[StrField] = fields.field(SubPropertyOf)

  def withName(name: String): PropertyTerm               = set(Name, name)
  def withDisplayName(displayName: String): PropertyTerm = set(DisplayName, displayName)
  def withDescription(description: String): PropertyTerm = set(Description, description)
  def withRange(range: String): PropertyTerm             = set(Range, range)
  def withSubClassOf(superProperties: Seq[String]): PropertyTerm =
    set(SubPropertyOf, AmfArray(superProperties.map(AmfScalar(_))))
}

case class ObjectPropertyTerm(fields: Fields, annotations: Annotations) extends PropertyTerm {
  override def meta: ObjectPropertyTermModel.type = ObjectPropertyTermModel
}

object ObjectPropertyTerm {

  def apply(): ObjectPropertyTerm = apply(Annotations())

  def apply(ast: YMap): ObjectPropertyTerm = apply(Annotations(ast))

  def apply(annotations: Annotations): ObjectPropertyTerm = ObjectPropertyTerm(Fields(), annotations)
}

case class DatatypePropertyTerm(fields: Fields, annotations: Annotations) extends PropertyTerm {
  override def meta: DatatypePropertyTermModel.type = DatatypePropertyTermModel
}

object DatatypePropertyTerm {

  def apply(): DatatypePropertyTerm = apply(Annotations())

  def apply(ast: YMap): DatatypePropertyTerm = apply(Annotations(ast))

  def apply(annotations: Annotations): DatatypePropertyTerm = DatatypePropertyTerm(Fields(), annotations)
}
