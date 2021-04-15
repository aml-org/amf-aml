package amf.plugins.document.vocabularies.model.document

import amf.core.errorhandling.ErrorHandler
import amf.core.metamodel.Obj
import amf.core.model.StrField
import amf.core.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, Fields}
import amf.core.traversal.{DomainElementSelectorAdapter, DomainElementTransformationAdapter, TransformationData, TransformationTraversal}
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceModel
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceModel._

case class DialectInstance(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with DeclaresModel
    with EncodesModel
    with ComposedInstancesSupport
    with PlatformSecrets {

  override def meta: Obj = DialectInstanceModel

  def encodes: DomainElement           = fields.field(Encodes)
  def references: Seq[BaseUnit]        = fields.field(References)
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)
  def definedBy(): StrField            = fields.field(DefinedBy)
  def declares: Seq[DomainElement]     = fields.field(Declares)

  override def componentId: String = ""

  def withDefinedBy(dialectId: String): DialectInstance =
    set(DefinedBy, dialectId)

  def withGraphDependencies(ids: Seq[String]): DialectInstance =
    set(GraphDependencies, ids)

  override def transform(selector: DomainElement => Boolean,
                         transformation: (DomainElement, Boolean) => Option[DomainElement])(
      implicit errorHandler: ErrorHandler): BaseUnit = {
    val domainElementAdapter  = new DomainElementSelectorAdapter(selector)
    val transformationAdapter = new DomainElementTransformationAdapter(transformation)
    new TransformationTraversal(TransformationData(domainElementAdapter, transformationAdapter, isSelfEncoded)).traverse(this)
    this
  }

  private def isSelfEncoded: Option[String] = {
    if (encodes.id == id) {
      Some(id)
    } else {
      None
    }
  }

}

object DialectInstance {
  def apply(): DialectInstance = apply(Annotations())

  def apply(annotations: Annotations): DialectInstance =
    DialectInstance(Fields(), annotations)
}

