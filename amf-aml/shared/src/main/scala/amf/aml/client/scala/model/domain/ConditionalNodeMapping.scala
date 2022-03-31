package amf.aml.client.scala.model.domain

import amf.aml.internal.metamodel.domain.{AnyMappingModel, ConditionalNodeMappingModel}
import amf.aml.internal.metamodel.domain.ConditionalNodeMappingModel._
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.{DomainElement, Linkable}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.internal.utils._
import org.yaml.model.YMap

case class ConditionalNodeMapping(override val fields: Fields, override val annotations: Annotations)
    extends AnyMapping(fields)
    with Linkable
    with MergeableMapping
    with NodeMappable[ConditionalNodeMappingModel.type] {

  override def meta: ConditionalNodeMappingModel.type = ConditionalNodeMappingModel

  def ifMapping: StrField   = fields.field(If)
  def thenMapping: StrField = fields.field(Then)
  def elseMapping: StrField = fields.field(Else)

  def withIfMapping(ifMapping: String): ConditionalNodeMapping     = set(If, ifMapping)
  def withThenMapping(thenMapping: String): ConditionalNodeMapping = set(Then, thenMapping)
  def withElseMapping(elseMapping: String): ConditionalNodeMapping = set(Else, elseMapping)

  override def linkCopy(): ConditionalNodeMapping = ConditionalNodeMapping().withId(id)
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement =
    ConditionalNodeMapping.apply
  private[amf] override def componentId: String = "/" + name.value().urlComponentEncoded
}

object ConditionalNodeMapping {
  def apply(): ConditionalNodeMapping = apply(Annotations())

  def apply(ast: YMap): ConditionalNodeMapping = apply(Annotations(ast))

  def apply(annotations: Annotations): ConditionalNodeMapping = ConditionalNodeMapping(Fields(), annotations)
}
