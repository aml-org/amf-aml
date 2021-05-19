package amf.plugins.document.vocabularies.model.document

import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.StrField
import amf.core.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, Fields}
import amf.core.traversal.{
  DomainElementSelectorAdapter,
  DomainElementTransformationAdapter,
  TransformationData,
  TransformationTraversal
}
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceModel
import amf.plugins.document.vocabularies.metamodel.document.DialectInstanceModel._

case class DialectInstance(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with DeclaresModel
    with EncodesModel
    with ComposedInstancesSupport
    with PlatformSecrets {

  override def meta: DialectInstanceModel.type = DialectInstanceModel

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
      implicit errorHandler: AMFErrorHandler): BaseUnit = {
    val domainElementAdapter  = new DomainElementSelectorAdapter(selector)
    val transformationAdapter = new DomainElementTransformationAdapter(transformation)
    val transformationData    = TransformationData(domainElementAdapter, transformationAdapter)
    val multiVisitAllowed     = if (isSelfEncoded) Set(id) else Set.empty[String]
    new TransformationTraversal(transformationData, multiVisitAllowed).traverse(this)
    this
  }

  private def isSelfEncoded: Boolean = encodes.id == id
}

object DialectInstance {
  def apply(): DialectInstance = apply(Annotations())

  def apply(annotations: Annotations): DialectInstance =
    DialectInstance(Fields(), annotations)
}
